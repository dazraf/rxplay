package fuzz.rxplay;

import com.google.common.util.concurrent.Futures;
import fuzz.rxplay.util.Factory0;

import java.util.concurrent.Future;

public class FlakyConnectionFactory<T> implements Factory0<Future<T>> {
  private final Factory0<T> resultFactory;
  private int failBeforeSucceed;

  public static <T> FlakyConnectionFactory<T> create(int failBeforeSuccess, Factory0<T> resultFactory) {
    return new FlakyConnectionFactory<>(failBeforeSuccess, resultFactory);
  }

  public FlakyConnectionFactory(int failBeforeSuccess, Factory0<T> resultFactory) {
    this.failBeforeSucceed = failBeforeSuccess;
    this.resultFactory = resultFactory;
  }

  public Future<T> create() {
    if (failBeforeSucceed <= 0) {
      return Futures.immediateFuture(resultFactory.create());
    } else {
      --failBeforeSucceed;
      return Futures.immediateFailedFuture(new Exception("we fail this time. remaining: " + failBeforeSucceed));
    }
  }
}
