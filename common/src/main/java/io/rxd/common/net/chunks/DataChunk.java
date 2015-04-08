package io.rxd.common.net.chunks;

import com.mongodb.DBObject;
import com.mongodb.DefaultDBEncoder;
import com.mongodb.connection.ByteBufferBsonOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.rxd.common.domain.Document;
import io.rxd.common.net.Chunk;
import io.rxd.common.net.NettyByteBuf;

import java.io.IOException;
import java.util.UUID;

public class DataChunk extends Chunk {
  public final static byte CODE = 1;
  private final Document document;

  public DataChunk(UUID uuid, Document doc) {
    super(uuid);
    this.document = doc;
  }

  public DataChunk(ByteBuf buffer) throws IOException {
    super(buffer);
    ByteBufInputStream bbis = new ByteBufInputStream(buffer);
    DBObject bsonObject = bsonDecoder.readObject(bbis);
    this.document = Document.from(bsonObject);
  }

  @Override
  protected void internalSerialise(ByteBuf buffer) throws IOException {
    ByteBufferBsonOutput bbbo = new ByteBufferBsonOutput(size -> new NettyByteBuf(buffer));
    DefaultDBEncoder dbEncoder = new DefaultDBEncoder();
    dbEncoder.set(bbbo);
    dbEncoder.putObject(document);
  }

  @Override
  protected byte getChunkCode() {
    return CODE;
  }

  @Override
  public Document getDocument() {
    return document;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{uuid: '")
      .append(getCommandId())
      .append("', doc: {")
      .append(document)
      .append("}}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DataChunk) {
      DataChunk rhs = (DataChunk)obj;
      if (super.equals(rhs)) {
        return (document == rhs.document) || (document != null && document.equals(rhs.document));
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 31 + (document != null ? document.hashCode() : 0);
  }
}
