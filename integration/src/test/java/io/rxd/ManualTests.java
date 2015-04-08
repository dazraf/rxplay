package io.rxd;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.rxd.client.Client;
import io.rxd.common.domain.Document;
import io.rxd.common.domain.UpsertAllCommand;
import io.rxd.common.net.*;
import io.rxd.common.net.chunks.CommandRequestChunk;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rxd.server.EchoServerHandler;
import rxd.server.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

@Ignore
public class ManualTests {
  private static final Logger logger = LoggerFactory.getLogger(ManualTests.class);
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
      Chunk chunk = Chunk.create(new UpsertAllCommand("dbName", "collectionName"));
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
  public void commandInvocationTest() throws InterruptedException, ParseException {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    CommandDispatcher commandDispatcher = new CommandDispatcher();

    CountDownLatch latch = new CountDownLatch(4);
    registerEchoCommandHandler(commandDispatcher, latch);

    ServerBootstrap serverBootstrap = BootstrapFactory.createServer(bossGroup, workerGroup, new ChannelHandler[]{
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      new ChunkByteBufCodec(),
      new CommandDataMUX("server"),
      commandDispatcher
    });

    ChannelFuture serverFuture = serverBootstrap.bind(PORT).sync(); // (7)

    EventLoopGroup clientGroup = new NioEventLoopGroup();

    Bootstrap clientBootstrap = BootstrapFactory.createClient(clientGroup, new ChannelHandler[]{
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      new ChunkByteBufCodec(),
      new CommandDataMUX("client"),
      new SimpleChannelInboundHandler<Object>() {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
          logger.info("got {}", msg.toString());
        }
      }
    });

    Channel channel = clientBootstrap.connect("localhost", PORT).sync().channel();
    try {
      UpsertAllCommand upsertAllCommand = new UpsertAllCommand("databaseName", "collectionName");
      channel.writeAndFlush(upsertAllCommand).sync();
      upsertAllCommand.incoming().subscribe(
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
      upsertAllCommand.outgoing().onNext(createDocument(0));
      upsertAllCommand.outgoing().onCompleted();
      latch.await();
    } finally {
      channel.close();
      serverFuture.channel().close();
      clientGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  private void registerEchoCommandHandler(CommandDispatcher commandDispatcher, CountDownLatch latch) {
    commandDispatcher.registerCommandHandler(UpsertAllCommand.class, command -> {
      command.incoming().subscribe(
        next -> {
          logger.info("Echo Handler onNext {}", next);
          command.outgoing().onNext(next);
          latch.countDown();
        },
        error -> {
          logger.info("Echo Handler onError {}", error);
          command.outgoing().onError(error);
          latch.countDown();
        },
        () -> {
          logger.info("Echo Handler onCompleted");
          command.outgoing().onCompleted();
          latch.countDown();
        }
      );
    });
  }

  private Document createADocument() throws ParseException {
    return Document.parse("{ 'name': {'first': 'Aris', 'last': 'Pez' } }");
  }

  private Document createDocument(int id) {
    try {
      return Document.parse("{ 'id': " + id + ", 'name': {'first': 'Aris', 'last': 'Pez' } }");
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

}
