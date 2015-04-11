package rxd.server;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.rxd.common.domain.Command;
import io.rxd.common.net.CommandDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;
import rxd.server.injection.ServerModule;

import static rxd.server.injection.ServerModule.CHILD_EVENTLOOP_GROUP;
import static rxd.server.injection.ServerModule.PARENT_EVENTLOOP_GROUP;

public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);
  private final ServerBootstrap bootstrap;
  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final CommandDispatcher commandDispatcher;
  private Channel channel;

  @Inject
  public Server(ServerBootstrap bootstrap,
                @Named(PARENT_EVENTLOOP_GROUP) EventLoopGroup bossGroup,
                @Named(CHILD_EVENTLOOP_GROUP) EventLoopGroup workerGroup,
                CommandDispatcher commandDispatcher) {
    this.bootstrap = bootstrap;
    this.bossGroup = bossGroup;
    this.workerGroup = workerGroup;
    this.commandDispatcher = commandDispatcher;
  }

  public void start(int port) throws Exception {
    // Bind and start to accept results connections.
    logger.info("starting on port {}...", port);
    ChannelFuture f = bootstrap.bind(port).sync(); // (7)
    logger.info("bound to port {}", port);
    channel = f.channel();
    logger.info("started");
  }

  public void stop() {
    logger.info("stopping ..");
    workerGroup.shutdownGracefully();
    bossGroup.shutdownGracefully();
    logger.info("stopped");
  }

  public <T extends Command> void registerCommandHandler(Class<T> klass, Action1<T> callback) {
    this.commandDispatcher.registerCommandHandler(klass, callback);
  }

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new ServerModule());
    Server server = injector.getInstance(Server.class);
    int port;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    } else {
      port = 8080;
    }
    try {
      server.start(port);
      server.blockUntilDisconnected(); // will never return
    } finally {
      server.stop();
    }
  }

  private void blockUntilDisconnected() throws InterruptedException {
    channel.closeFuture().sync();
  }
}
