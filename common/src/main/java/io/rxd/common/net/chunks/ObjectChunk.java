package io.rxd.common.net.chunks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.rxd.common.domain.Domain;
import io.rxd.common.net.Chunk;

import java.io.IOException;
import java.util.UUID;

public class ObjectChunk extends Chunk<Domain> {
  public final static byte CODE = 4;
  private final Domain object;

  public ObjectChunk(ByteBuf buffer) throws IOException {
    super(buffer);
    this.object = mapper.readValue(new ByteBufInputStream(buffer), Domain.class);
  }

  public ObjectChunk(UUID commandId, Domain object) {
    super(commandId);
    this.object = object;
  }

  @Override
  protected void internalSerialise(ByteBuf buffer) throws IOException {
    mapper.writeValue(new ByteBufOutputStream(buffer), object);
  }

  @Override
  protected byte getChunkCode() {
    return CODE;
  }

  @Override
  public Domain get() {
    return object;
  }
}
