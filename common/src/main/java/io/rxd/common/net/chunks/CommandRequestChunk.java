package io.rxd.common.net.chunks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.rxd.common.domain.Command;
import io.rxd.common.domain.Domain;
import io.rxd.common.net.Chunk;

import java.io.IOException;

public class CommandRequestChunk extends Chunk<Command> {
  public final static byte CODE = 0;
  private final Command command;

  public CommandRequestChunk(Command command) {
    super(command.getId());
    this.command = command;
  }

  public CommandRequestChunk(ByteBuf buffer) throws IOException {
    super(buffer);
    this.command = (Command)mapper.readValue(new ByteBufInputStream(buffer), Domain.class);
    this.command.setId(getCommandId());
  }

  @Override
  protected void internalSerialise(ByteBuf buffer) throws IOException {
    mapper.writeValue(new ByteBufOutputStream(buffer), command);
  }

  @Override
  protected byte getChunkCode() {
    return CODE;
  }

  @Override
  public Command get() {
    return command;
  }

  @Override
  public String toString() {
    return "command-chunk-" + ( command != null ? command.toString() : "empty");
  }

}
