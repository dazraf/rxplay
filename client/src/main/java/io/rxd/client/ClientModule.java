package io.rxd.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.rxd.common.net.BootstrapFactory;
import io.rxd.common.net.ChunkByteBufCodec;
import io.rxd.common.net.CommandDataMUX;
import io.rxd.common.net.CommandDispatcher;

import static io.rxd.common.net.CommandDataMUX.Mode.CLIENT;

public class ClientModule extends AbstractModule {
  public static final String CLIENT_EVENTLOOP_GROUP = "clientEventLoopGroup";

  @Override
  protected void configure() {
    bind(CommandDataMUX.Mode.class).toInstance(CLIENT);
    bind(CommandDataMUX.class).in(Singleton.class);
    bind(ChunkByteBufCodec.class).in(Singleton.class);
    bind(CommandDispatcher.class).in(Singleton.class);
    bind(SimpleClientHandler.class).in(Singleton.class);
    bind(EventLoopGroup.class)
      .annotatedWith(Names.named(CLIENT_EVENTLOOP_GROUP))
      .to(NioEventLoopGroup.class)
      .in(Singleton.class);
  }

  @Singleton
  @Provides
  private Bootstrap provideClientBootstrap(
    @Named(CLIENT_EVENTLOOP_GROUP) EventLoopGroup workerGroup,
    ChunkByteBufCodec chunkByteBufCodec,
    CommandDataMUX commandDataMUX,
    SimpleClientHandler simpleClientHandler) {
    return BootstrapFactory.createClient(workerGroup, new ChannelHandler[]{
      new LengthFieldPrepender(4),
      new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
      chunkByteBufCodec,
      commandDataMUX,
      simpleClientHandler
    });
  }
}
