package rxd.server.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.rxd.common.domain.EchoCommand;
import io.rxd.common.domain.UpsertAllCommand;
import io.rxd.common.net.BootstrapFactory;
import io.rxd.common.net.ChunkByteBufCodec;
import io.rxd.common.net.CommandDataMUX;
import io.rxd.common.net.CommandDispatcher;
import rxd.server.Server;
import rxd.server.handlers.EchoCommandHandler;
import rxd.server.handlers.UpsertAllCommandHandler;

import static io.rxd.common.net.CommandDataMUX.Mode.SERVER;

public class ServerModule extends AbstractModule {
  public final static String PARENT_EVENTLOOP_GROUP = "parentEventLoopGroup";
  public final static String CHILD_EVENTLOOP_GROUP = "childEventLoopGroup";

  @Override
  protected void configure() {
    bind(CommandDataMUX.Mode.class).toInstance(SERVER);
    bind(CommandDataMUX.class);
    bind(EventLoopGroup.class)
      .annotatedWith(Names.named(PARENT_EVENTLOOP_GROUP))
      .to(NioEventLoopGroup.class)
      .in(Singleton.class);
    bind(EventLoopGroup.class)
      .annotatedWith(Names.named(CHILD_EVENTLOOP_GROUP))
      .to(NioEventLoopGroup.class)
      .in(Singleton.class);
    bind(ChunkByteBufCodec.class).in(Singleton.class);
    bind(Server.class).in(Singleton.class);
  }

  @Singleton
  @Provides
  private ServerBootstrap provideServerBootstrap(@Named(PARENT_EVENTLOOP_GROUP) EventLoopGroup parentGroup,
                                                 @Named(CHILD_EVENTLOOP_GROUP) EventLoopGroup childGroup,
                                                 ChunkByteBufCodec chunkedCodec,
                                                 CommandDataMUX serverMUX,
                                                 CommandDispatcher commandDispatcher) {
    ServerBootstrap bootstrap = BootstrapFactory.createServer(parentGroup, childGroup, new ChannelHandler[]{
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      chunkedCodec,
      serverMUX,
      commandDispatcher
    });
    return bootstrap;
  }

  @Singleton
  @Provides
  private CommandDispatcher provideCommandDispatcher() {
    return registerCommands(new CommandDispatcher());
  }

  protected CommandDispatcher registerCommands(CommandDispatcher commandDispatcher) {
    commandDispatcher.registerCommandHandler(EchoCommand.class, new EchoCommandHandler());
    commandDispatcher.registerCommandHandler(UpsertAllCommand.class, new UpsertAllCommandHandler());
    return commandDispatcher;
  }
}
