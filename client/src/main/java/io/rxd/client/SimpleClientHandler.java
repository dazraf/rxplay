package io.rxd.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleClientHandler extends SimpleChannelInboundHandler<Object> {
  private static final Logger logger = LoggerFactory.getLogger(SimpleClientHandler.class);
  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object object) throws Exception {
    logger.info("got an object {}", object.toString());
  }
}
