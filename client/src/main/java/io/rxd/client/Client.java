package io.rxd.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.rxd.common.domain.Document;
import io.rxd.common.domain.UpsertAllCommand;
import io.rxd.common.net.BootstrapFactory;
import io.rxd.common.net.ChunkByteBufCodec;
import io.rxd.common.net.CommandDataMUX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.rxd.common.net.CommandDataMUX.Mode.CLIENT;

public class Client {
  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  private String host;
  private int port;
  private Channel channel;
  private EventLoopGroup workerGroup = new NioEventLoopGroup();

  public Client(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public static void main(String[] args) throws Exception {
    String host = "localhost";
    int port;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    } else {
      port = 8080;
    }
    Client client = new Client(host, port);
    try {
      client.start();

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

  public void start() throws InterruptedException {
    logger.info("setting up bootstrap");
    Bootstrap bootstrap = BootstrapFactory.createClient(workerGroup, new ChannelHandler[] {
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      new ChunkByteBufCodec(),
      new CommandDataMUX(CLIENT),
      new SimpleClientHandler()
    });

    logger.info("setup bootstrap");

    // Start the client.
    logger.info("connecting to {}:{}", host, port);
    ChannelFuture f = bootstrap.connect(host, port).sync(); // (5)
    logger.info("connected to {}:{}", host, port);
    channel = f.channel();
  }

  public void write(Object object) {
    channel.writeAndFlush(object);
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
