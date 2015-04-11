package rxd.server.handlers;

import io.rxd.common.domain.RecordKey;
import io.rxd.common.domain.UpsertAllCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

public class UpsertAllCommandHandler implements Action1<UpsertAllCommand> {
  private static final Logger logger = LoggerFactory.getLogger(UpsertAllCommandHandler.class);

  @Override
  public void call(UpsertAllCommand command) {
    try {
      logger.info("processing upsert command");
      command.resultsObserver().onNext(new RecordKey("A12", 1));
      command.resultsObserver().onCompleted();
      logger.info("finished upsert command");
    } catch (Exception e) {
      logger.error("failed to process upsert command", e);
      command.resultsObserver().onError(e);
    }
  }
}
