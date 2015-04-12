package io.rxd.client;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.rxd.common.domain.Document;
import io.rxd.common.domain.UpsertAllCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
  private static final Logger logger = LoggerFactory.getLogger(Client.class);
  private final Bootstrap bootstrap;
  private final EventLoopGroup workerGroup;
  private Channel channel;

  @Inject
  public Client(Bootstrap bootstrap,
                @Named(ClientModule.CLIENT_EVENTLOOP_GROUP) EventLoopGroup workerGroup) {
    this.bootstrap = bootstrap;
    this.workerGroup = workerGroup;
  }

  public static void main(String[] args) throws Exception {
    String host = "localhost";
    int port;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    } else {
      port = 8080;
    }
    Injector injector = Guice.createInjector(new ClientModule());
    Client client = injector.getInstance(Client.class);
    try {
      client.start(host, port);

      UpsertAllCommand command = new UpsertAllCommand().withDatabaseName("life").withCollectionName("people");
      Document doc = Document.parse("{ 'name': {'first': 'Aris', 'last': 'Pez' } }");
      command.results().subscribe(
        System.out::println,
        (Throwable t) -> System.out.println("failed"),
        () -> System.out.println("completed")
      );

      client.write(command);
      client.blockUntilDisconnected();
    } finally {
      client.stop();
    }
  }

  public void start(String host, int port) throws InterruptedException {
    // Start the client.
    logger.info("connecting to {}:{}...", host, port);
    ChannelFuture f = bootstrap.connect(host, port).sync();
    logger.info("connected to {}:{}", host, port);
    channel = f.channel();
  }

  public void write(Object object) throws InterruptedException {
    channel.writeAndFlush(object).sync();
  }

  public void stop() {
    logger.info("stopping");
    workerGroup.shutdownGracefully();
    logger.info("stopped");
  }

  private void blockUntilDisconnected() throws InterruptedException {
    // Wait until the connection is closed.
    channel.closeFuture().sync();
  }
}
