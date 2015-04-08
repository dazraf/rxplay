package io.rxd.common.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChunkByteBufCodec extends ByteToMessageCodec<Chunk> {
  private static final Logger logger = LoggerFactory.getLogger(ChunkByteBufCodec.class);

  @Override
  protected void encode(ChannelHandlerContext ctx, Chunk msg, ByteBuf out) throws Exception {
    logger.info("encoding chunk to byte buffer {} ", msg.getCommandId());
    msg.serialise(out);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    Chunk chunk = Chunk.deserialise(in);
    out.add(chunk);
    logger.info("decoded bytebuffer to chunk buffer {} ", chunk.getCommandId());
  }
}
