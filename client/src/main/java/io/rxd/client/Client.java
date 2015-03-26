package io.rxd.client;

import com.mongodb.BasicDBObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.rxd.common.net.BSONObjectToByteEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {
  public static void main(String[] args) throws Exception {
    String host = "localhost";
    int port;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    } else {
      port = 8080;
    }
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      Bootstrap b = new Bootstrap(); // (1)
      b.group(workerGroup); // (2)
      b.channel(NioSocketChannel.class); // (3)
      b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
      b.handler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
          ch.pipeline()
            .addLast(new BSONObjectToByteEncoder())
            .addLast(new ClientHandler());
        }
      });

      // Start the client.
      ChannelFuture f = b.connect(host, port).sync(); // (5)
      boolean terminate = false;
      while (!terminate) {
        BasicDBObject obj = new BasicDBObject();
        obj.put("siblings", 2);
        f.channel().writeAndFlush(obj);
        terminate = new BufferedReader(new InputStreamReader(System.in)).readLine().length() > 0;
      }
      // f.channel().writeAndFlush(ByteBufUtil.encodeString(f.channel().alloc(), CharBuffer.wrap("hello"), Charset.defaultCharset()));
      // Wait until the connection is closed.
      f.channel().closeFuture().sync();
    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
