package io.rxd.common.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class BootstrapFactory {
  public static Bootstrap createClient(EventLoopGroup workerGroup, ChannelHandler[] channelHanders) {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(workerGroup);
    bootstrap.channel(NioSocketChannel.class);
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel socketChannel) throws Exception {
        for (ChannelHandler handler : channelHanders) {
          socketChannel.pipeline().addLast(handler);
        }
      }
    });
    return bootstrap;
  }

  public static ServerBootstrap createServer(EventLoopGroup parentGroup, EventLoopGroup childGroup, ChannelHandler[] channelHandlers) {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(parentGroup, childGroup)
      .channel(NioServerSocketChannel.class)
      .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
          for (ChannelHandler handler : channelHandlers) {
            ch.pipeline().addLast(handler);
          }
        }
      })
      .option(ChannelOption.SO_BACKLOG, 128)
      .childOption(ChannelOption.SO_KEEPALIVE, true);
    return bootstrap;
  }

}
