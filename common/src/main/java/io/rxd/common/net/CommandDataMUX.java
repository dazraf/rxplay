package io.rxd.common.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.rxd.common.domain.Command;
import io.rxd.common.domain.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The purpose of this codec is two fold:
 * <p>
 * A. If requested to encode a {@link Command} it converts it to a Chunk
 * B. If requested to decode a {@link io.rxd.common.net.chunks.CommandRequestChunk} it decodes the command and passes it inbound
 * <p>
 * In both cases, it will:
 * 1. Bind incoming protocol chunks to the incoming observer
 * 2. Bind outgoing observable stream to the outbound stream
 * <p>
 * One way to visualise this
 * <p>
 *     +--------+          +-----------------+
 *      |  ...   |          |Command Processor|
 *      +-^------+          +--^--------------+
 *        |   |                |          |
 *        +   v                |          |
 *      <Commands>             |          |
 *        ^   +                +  Command v
 *        |   |             [incoming][outgoing]
 *   +--------v-----+          ^          +
 *   |CommandDataMUX+----------+          |
 *   |              <---------------------+
 *   +----^---------+
 *        +   v
 *       <Chunks>
 *        ^   +
 *        |   |
 *  +---------v-------+
 *  |ChunkByteBufCodec|
 *  +-----------------+
 */
public class CommandDataMUX extends MessageToMessageCodec<Chunk, Command> {
  private static final Logger logger = LoggerFactory.getLogger(CommandDataMUX.class);

  private ConcurrentHashMap<UUID, Observer<Document>> cachedIncomingObservers = new ConcurrentHashMap<>();
  private final String name;

  public CommandDataMUX (String name) {
    this.name = name;
  }
  /**
   * this is called on the client-side
   *
   * @param ctx
   * @param cmd
   * @param out
   * @throws Exception
   */
  @Override
  protected void encode(ChannelHandlerContext ctx, Command cmd, List<Object> out) throws Exception {
    try {
      cmd.prepare();
      logger.info("{} encoding for command {}", name, cmd.getId());
      out.add(Chunk.create(cmd));
      bindCommand(ctx, cmd);
    } catch (Exception e) {
      clearCachedCommand(cmd.getId());
      throw e;
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, Chunk chunk, List<Object> out) throws Exception {
    if (chunk.isCommandRequest()) {
      logger.info("{} decoding command: {}", name, chunk.getCommandId());
      bindCommand(ctx, chunk.getCommand());
      out.add(chunk.getCommand());
    } else if (chunk.isData()) {
      logger.info("{} decoding data chunk for: {}", name, chunk.getCommandId());
      findIncomingObserver(chunk).onNext(chunk.getDocument());
    } else if (chunk.isException()) {
      logger.info("{} decoding exception for: {}", name, chunk.getCommandId());
      findIncomingObserver(chunk).onError(chunk.getThrowable());
      clearCachedCommand(chunk.getCommandId());
    } else if (chunk.isCompleted()) {
      logger.info("{} decoding completion for: {}", name, chunk.getCommandId());
      findIncomingObserver(chunk).onCompleted();
      clearCachedCommand(chunk.getCommandId());
    } else {
      throw new ProtocolException("unknown chunk type: " + Byte.toString(chunk.getChunkCode()));
    }
  }

  private void bindCommand(ChannelHandlerContext ctx, Command cmd) throws ProtocolException {
    logger.info("{} binding command {}", name, cmd.getId());
    cacheCommand(cmd);
    // listen for outgoing data packets
    // and send them as chunks
    // in theory we can perform sub chunking here
    cmd.outgoingObservable().subscribe(
      (Document document) -> {
        logger.info("{} transmitting document for: {} ", name, cmd.getId());
        ctx.channel().writeAndFlush(Chunk.create(cmd.getId(), document));
      }, // command result
      (Throwable throwable) -> {
        logger.info("{} transmitting exception for: {}", name, cmd.getId());
        ctx.channel().writeAndFlush(Chunk.create(cmd.getId(), throwable));
      }, // command failesd
      () -> {
        logger.info("{} transmitting completion for: {}", name, cmd.getId());
        ctx.channel().writeAndFlush(Chunk.create(cmd.getId()));
      });
  }


  private Observer<Document> findIncomingObserver(Chunk chunk) throws ProtocolException {
    Observer<Document> observer = cachedIncomingObservers.get(chunk.getCommandId());
    if (observer == null) {
      throw new ProtocolException("could not find incoming observer for command " + chunk.getCommandId());
    }
    return observer;
  }

  private void clearCachedCommand(UUID commandId) {
    cachedIncomingObservers.remove(commandId);
  }

  private void cacheCommand(Command cmd) throws ProtocolException {
    logger.info("{} caching command {}", name, cmd.getId());
    UUID id = cmd.getId();
    if (cachedIncomingObservers.containsKey(id)) {
      logger.error("{} duplicate command id found whilst caching command {}", name, id);
      throw new ProtocolException("duplicate command id: " + id.toString());
    }
    cachedIncomingObservers.put(id, cmd.incomingObserver());
  }
}
