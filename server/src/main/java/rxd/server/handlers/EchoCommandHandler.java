package rxd.server.handlers;

import io.rxd.common.domain.EchoCommand;
import rx.functions.Action1;

public class EchoCommandHandler implements Action1<EchoCommand> {
  @Override
  public void call(EchoCommand echoCommand) {
    echoCommand.parametersObservable().subscribe(
      doc -> echoCommand.resultsObserver().onNext(doc),
      error -> echoCommand.resultsObserver().onError(error),
      () -> echoCommand.resultsObserver().onCompleted());
  }
}
