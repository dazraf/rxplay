package rxd.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.bson.BSONObject;

public class ClientHandler extends SimpleChannelInboundHandler<BSONObject> {
  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, BSONObject bsonObject) throws Exception {
    String str = bsonObject.toString();
    System.out.println(str);
  }
}
