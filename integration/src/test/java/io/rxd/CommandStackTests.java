package io.rxd;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.rxd.client.Client;
import io.rxd.client.ClientModule;
import io.rxd.common.domain.Document;
import io.rxd.common.domain.EchoCommand;
import io.rxd.common.domain.UpsertAllCommand;
import io.rxd.common.net.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rxd.server.Server;
import rxd.server.handlers.EchoServerHandler;
import rxd.server.injection.ServerModule;

import java.text.ParseException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class CommandStackTests {
  private static final Logger logger = LoggerFactory.getLogger(CommandStackTests.class);
  private static final int PORT = 8080;

  @Test
  public void dataChunkTransferTest() throws InterruptedException, ParseException {

    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    ServerBootstrap serverBootstrap = BootstrapFactory.createServer(bossGroup, workerGroup, new ChannelHandler[]{
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      new ChunkByteBufCodec(),
      new EchoServerHandler()
    });
    ChannelFuture serverFuture = serverBootstrap.bind(PORT).sync(); // (7)

    EventLoopGroup clientGroup = new NioEventLoopGroup();
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Chunk> receivedChunk = new AtomicReference<>();

    Bootstrap clientBootstrap = BootstrapFactory.createClient(clientGroup, new ChannelHandler[]{
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      new ChunkByteBufCodec(),
      new SimpleChannelInboundHandler<Object>() {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
          logger.info("got {}", msg.toString());
          receivedChunk.set((Chunk) msg);
          latch.countDown();
        }
      }
    });

    Channel channel = clientBootstrap.connect("localhost", PORT).sync().channel();
    try {
      Chunk chunk = Chunk.create(UUID.randomUUID(), createADocument());
      channel.writeAndFlush(chunk);
      latch.await();
      assertEquals(receivedChunk.get(), chunk);
    } finally {
      channel.close();
      serverFuture.channel().close();
      clientGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  @Test
  public void commandChunkTransferTest() throws InterruptedException, ParseException {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    ServerBootstrap serverBootstrap = BootstrapFactory.createServer(bossGroup, workerGroup, new ChannelHandler[]{
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      new ChunkByteBufCodec(),
      new EchoServerHandler()
    });
    ChannelFuture serverFuture = serverBootstrap.bind(PORT).sync();

    EventLoopGroup clientGroup = new NioEventLoopGroup();
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Chunk> receivedChunk = new AtomicReference<>();

    Bootstrap clientBootstrap = BootstrapFactory.createClient(clientGroup, new ChannelHandler[]{
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      new ChunkByteBufCodec(),
      new SimpleChannelInboundHandler<Object>() {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
          logger.info("got {}", msg.toString());
          receivedChunk.set((Chunk) msg);
          latch.countDown();
        }
      }
    });

    Channel channel = clientBootstrap.connect("localhost", PORT).sync().channel();
    try {
      Chunk chunk = Chunk.create(new UpsertAllCommand().withDatabaseName("dbName").withCollectionName("collectionName"));
      channel.writeAndFlush(chunk);
      latch.await();
      assertEquals(receivedChunk.get(), chunk);
    } finally {
      channel.close();
      serverFuture.channel().close();
      clientGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  @Test
  public void commandInvocationTest() throws Exception {
    Injector serverInjector = Guice.createInjector(new ServerModule());
    Server server = serverInjector.getInstance(Server.class);

    CountDownLatch latch = new CountDownLatch(4);
    registerEchoCommandHandler(serverInjector.getInstance(CommandDispatcher.class), latch);
    server.start(PORT);

    Injector clientInjector = Guice.createInjector(new ClientModule());
    Client client = clientInjector.getInstance(Client.class);
    client.start("localhost", PORT);
    try {
      EchoCommand echoCommand = new EchoCommand().withDatabaseName("databaseName").withCollectionName("collectionName");
      client.write(echoCommand);
      echoCommand.results().subscribe(
        next -> {
          logger.info("client received: {}", next);
          latch.countDown();
        },
        error -> {
          logger.error("client received command error: {}", error);
          latch.countDown();
        },
        () -> {
          logger.info("client recieved command completed");
          latch.countDown();
        }
      );
      echoCommand.parameters().onNext(createADocument(0));
      echoCommand.parameters().onCompleted();
      latch.await();
    } finally {
      client.stop();
      server.stop();
    }
  }

  private void registerEchoCommandHandler(CommandDispatcher commandDispatcher, CountDownLatch latch) {
    logger.info("registering echo command handler");
    commandDispatcher.removeCommandHandler(EchoCommand.class);
    commandDispatcher.registerCommandHandler(EchoCommand.class, command -> {
      command.parametersObservable().subscribe(
        next -> {
          logger.info("Echo Handler onNext {}", next);
          command.resultsObserver().onNext(next);
          latch.countDown();
        },
        error -> {
          logger.info("Echo Handler onError {}", error);
          command.resultsObserver().onError(error);
          latch.countDown();
        },
        () -> {
          logger.info("Echo Handler onCompleted");
          command.resultsObserver().onCompleted();
          latch.countDown();
        }
      );
    });
  }

  private Document createADocument() throws ParseException {
    return Document.parse("{ 'name': {'first': 'Aris', 'last': 'Pez' } }");
  }

  private Document createADocument(int id) {
    try {
      return Document.parse("{ 'id': " + id + ", 'name': {'first': 'Aris', 'last': 'Pez' } }");
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
