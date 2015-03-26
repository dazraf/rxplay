package rxd.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler extends SimpleChannelInboundHandler<BSONObject> {
  private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, BSONObject bsonObject) throws Exception {
    System.out.println(bsonObject.toString());
    ctx.writeAndFlush(bsonObject) // echo it back
      .addListener(f -> {
        if (f.isDone()) {
          if (f.isSuccess()) {
            System.out.println("success");
          } else {
            System.out.println("failed " + f.cause().getMessage());
          }
        }
      });
  }
}
