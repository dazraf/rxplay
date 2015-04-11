package io.rxd.common.net.chunks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.rxd.common.net.Chunk;

import java.io.IOException;
import java.util.UUID;

public class ExceptionChunk extends Chunk<Throwable> {
  public final static byte CODE = 3;
  private final Throwable throwable;

  public ExceptionChunk(UUID commandId, Throwable throwable) {
    super(commandId);
    this.throwable = throwable;
  }

  public ExceptionChunk(ByteBuf buffer) throws IOException {
    super(buffer);
    this.throwable = new Exception(new ByteBufInputStream(buffer).readUTF());
  }

  @Override
  protected void internalSerialise(ByteBuf buffer) throws IOException {
    new ByteBufOutputStream(buffer).writeUTF(this.throwable.getMessage());
  }

  @Override
  protected byte getChunkCode() {
    return CODE;
  }

  @Override
  public Throwable get() {
    return throwable;
  }
}
