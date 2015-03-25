package fuzz.rxplay;

import fuzz.rxplay.util.Factory0;
import rx.Observable;

public class FlakyResultObservableFactory implements Factory0<Observable<Integer>> {
  public Observable<Integer> create() {
    return Observable.create(s -> {
      for (int i = 0; i < 3; ++i) {
        s.onNext(i);
      }
      // now we block long enough to cause a time out
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        s.onError(e);
      }
    });
  }
}
