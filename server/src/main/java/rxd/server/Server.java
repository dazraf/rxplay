package rxd.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.rxd.common.domain.Command;
import io.rxd.common.domain.Document;
import io.rxd.common.domain.UpsertAllCommand;
import io.rxd.common.net.BootstrapFactory;
import io.rxd.common.net.ChunkByteBufCodec;
import io.rxd.common.net.CommandDataMUX;
import io.rxd.common.net.CommandDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);
  private int port;
  private EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
  private EventLoopGroup workerGroup = new NioEventLoopGroup();
  private ChunkByteBufCodec chunkedCodec = new ChunkByteBufCodec();
  private CommandDataMUX commandDataMUX = new CommandDataMUX("server");
  private CommandDispatcher commandDispatcher = new CommandDispatcher();
  private Channel channel;

  public Server(int port) {
    this.port = port;
  }

  public void start() throws Exception {
    logger.info("setting up bootstrap");
    ServerBootstrap bootstrap = BootstrapFactory.createServer(bossGroup, workerGroup, new ChannelHandler[] {
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      chunkedCodec,
      commandDataMUX,
      commandDispatcher
    });

    // Bind and start to accept incoming connections.
    logger.info("binding to port {}", port);
    ChannelFuture f = bootstrap.bind(port).sync(); // (7)
    logger.info("bound to port {}", port);
    channel = f.channel();
    logger.info("setup");
  }

  public <T extends Command> void registerCommandHandler(Class<T> klass, Action1<T> handler) {
    commandDispatcher.registerCommandHandler(klass, handler);
  }

  public void stop() {
    logger.info("setting up bootstrap");

    workerGroup.shutdownGracefully();
    bossGroup.shutdownGracefully();
  }

  public static void main(String[] args) throws Exception {
    int port;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    } else {
      port = 8080;
    }
    Server server = new Server(port);
    try {
      server.start();
      server.registerCommandHandler(UpsertAllCommand.class, c -> {
        try {
          logger.info("processing upsert command");
          c.outgoing().onNext(Document.parse("{ 'result': 1}"));
          c.outgoing().onCompleted();
          logger.info("finished upsert command");
        } catch (Exception e) {
          logger.error("failed to process upsert command", e);
          c.outgoing().onError(e);
        }
      });
      server.blockUntilDisconnected(); // will never return
    } finally {
      server.stop();
    }
  }

  private void blockUntilDisconnected() throws InterruptedException {
    channel.closeFuture().sync();
  }
}
