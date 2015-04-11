package io.rxd.common.net.chunks;

import io.netty.buffer.ByteBuf;
import io.rxd.common.net.Chunk;

import java.io.IOException;
import java.util.UUID;

public class CommandCompletedChunk extends Chunk<Void> {
  public final static byte CODE = 2;

  public CommandCompletedChunk(UUID commandId) {
    super(commandId);
  }

  public CommandCompletedChunk(ByteBuf buffer) {
    super(buffer);
  }

  @Override
  protected void internalSerialise(ByteBuf buffer) throws IOException {
    // nothing to do here
  }

  @Override
  protected byte getChunkCode() {
    return CODE;
  }

  @Override
  public Void get() {
    return null;
  }
}
