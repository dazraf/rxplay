package io.rxd.common.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.rxd.common.domain.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.util.concurrent.ConcurrentHashMap;

public class CommandDispatcher extends SimpleChannelInboundHandler<Command> {
  private static final Logger logger = LoggerFactory.getLogger(CommandDispatcher.class);

  private ConcurrentHashMap<Class, Action1> handlers = new ConcurrentHashMap<>();

  public <T extends Command> CommandDispatcher registerCommandHandler(Class<T> klass, Action1<T> handler) {
    if (this.handlers.containsKey(klass)) {
      logger.error("hanlder already registered for command {}", klass.getName());
      throw new RxdFatalException("handler already registered for command " + klass.getName());
    }
    logger.info("adding command handler for {}", klass.getName());
    handlers.put(klass, handler);
    return this;
  }

  public <T extends Command> void removeCommandHandler(Class<T> klass) {
    logger.info("removing command handler for {}", klass.getName());
    this.handlers.remove(klass);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
    Action1 handler = handlers.get(msg.getClass());
    if (handler != null) {
      handler.call(msg);
    } else {
      logger.error("unsupported command: {}", msg.getClass().getName());
      throw new ProtocolException("command unsupported");
    }
  }
}
