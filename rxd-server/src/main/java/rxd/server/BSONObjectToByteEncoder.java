package rxd.server;

import com.mongodb.DBObject;
import com.mongodb.DefaultDBEncoder;
import com.mongodb.LazyDBDecoder;
import com.mongodb.connection.ByteBufferBsonOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.bson.LazyBSONCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BSONObjectToByteEncoder extends ByteToMessageCodec<DBObject> {
  private static final Logger logger = LoggerFactory.getLogger(BSONObjectToByteEncoder.class);
  private static final LazyDBDecoder bsonDecoder = new LazyDBDecoder();

  private static final LazyBSONCallback cb = new LazyBSONCallback();

  @Override
  protected void encode(ChannelHandlerContext ctx, DBObject msg, ByteBuf out) throws Exception {
    ByteBufferBsonOutput bbbo = new ByteBufferBsonOutput(size -> new NettyByteBuf(out));

    DefaultDBEncoder dbEncoder = new DefaultDBEncoder();

    dbEncoder.set(bbbo);
    dbEncoder.putObject(msg);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    ByteBufInputStream bbis = new ByteBufInputStream(in);
    DBObject bsonObject = bsonDecoder.readObject(bbis);
    out.add(bsonObject);
  }
}
