package io.rxd.common.net;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.LazyDBDecoder;
import de.undercouch.bson4jackson.BsonFactory;
import io.netty.buffer.ByteBuf;
import io.rxd.common.domain.Command;
import io.rxd.common.domain.Document;
import io.rxd.common.domain.Domain;
import io.rxd.common.net.chunks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public abstract class Chunk<T> {
  protected static final Logger logger = LoggerFactory.getLogger(Chunk.class);
  protected static final LazyDBDecoder bsonDecoder = new LazyDBDecoder();
  protected static final ObjectMapper mapper = new ObjectMapper(new BsonFactory());

  private UUID uuid;

  {
    mapper.enableDefaultTyping();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static Chunk create(Command command) {
    return new CommandRequestChunk(command.prepare());
  }

  public static Chunk create(UUID commandId, Document document) {
    return new DataChunk(commandId, document);
  }

  public static Chunk create(UUID commandId, Domain object) {
    if (object instanceof Command)
      return create(commandId, (Command)object);
    else if (object instanceof Document)
      return create(commandId, (Document)object);
    else
      return new ObjectChunk(commandId, object);
  }

  public static Chunk create( UUID commandId, Throwable exception) {
    return new ExceptionChunk(commandId, exception);
  }

  public static Chunk create(UUID commandId) {
    return new CommandCompletedChunk(commandId);
  }

  static Chunk deserialise(ByteBuf buffer) throws ProtocolException {
    try {
      byte b = buffer.readByte();
      switch (b) {
        case CommandRequestChunk.CODE:
          return new CommandRequestChunk(buffer);
        case DataChunk.CODE:
          return new DataChunk(buffer);
        case CommandCompletedChunk.CODE:
          return new CommandCompletedChunk(buffer);
        case ExceptionChunk.CODE:
          return new ExceptionChunk(buffer);
        case ObjectChunk.CODE:
          return new ObjectChunk(buffer);
        default:
          throw new ProtocolException("Serialisers don't match!");
      }
    } catch (IOException ioException) {
      throw new ProtocolException("I/O exception", ioException);
    }
  }


  final void serialise(ByteBuf buffer) throws ProtocolException {
    try {
      buffer.writeByte(getChunkCode());
      buffer.writeLong(uuid.getMostSignificantBits());
      buffer.writeLong(uuid.getLeastSignificantBits());
      // ByteBuf contents = buffer.slice(buffer.writerIndex(), buffer.capacity() - buffer.writerIndex());
      internalSerialise(buffer);
      // buffer.writerIndex(buffer.writerIndex() + contents.writerIndex());
    } catch (Exception e) {
      throw new ProtocolException("exception during serialisation", e);
    }
  }

  boolean isCommandRequest() {
    return this instanceof CommandRequestChunk;
  }

  boolean isData() {
    return this instanceof DataChunk;
  }

  boolean isObject() {
    return this instanceof ObjectChunk;
  }

  boolean isCompleted() {
    return this instanceof CommandCompletedChunk;
  }

  boolean isException() {
    return this instanceof ExceptionChunk;
  }

  public Throwable getThrowable() {
    return null;
  }

  public UUID getCommandId() {
    return uuid;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Chunk))
      return false;
    Chunk rhs = (Chunk)obj;
    return ((rhs.uuid == null && uuid == null) ||
      rhs.uuid != null && uuid != null && rhs.uuid.equals(uuid));
  }

  protected Chunk(UUID commandId) {
    this.uuid = commandId;
  }

  protected Chunk(ByteBuf buffer) {
    this.uuid = new UUID(buffer.readLong(), buffer.readLong());
  }

  protected abstract void internalSerialise(ByteBuf buffer) throws IOException;

  protected abstract byte getChunkCode();

  public abstract T get();
}
