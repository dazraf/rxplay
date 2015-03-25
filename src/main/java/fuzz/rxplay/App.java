package fuzz.rxplay;

import fuzz.rxplay.util.Factory0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) throws IOException {
    Factory0<Future<Observable<Integer>>> connectionFactory = FlakyConnectionFactory.create(2, new FlakyResultObservableFactory());

    // we re-execute the connection logic by having a BehaviourSubject
    // this reinvokes the closure on subscribe
    Observable<Observable<Integer>> connection = BehaviorSubject.create(h -> {
      logger.info("subscribed - getting the future");
      Observable.from(connectionFactory.create()).subscribe(h);
    });

    // now build a pipeline off that
    connection
      .doOnError(t -> logger.error("fail")) // just for logging - we should see the connection failures
      .retry(2)                             // give some grace to failed connections
      .flatMap(it -> it)                    // pop out the data observable from the future
      .timeout(500, TimeUnit.MILLISECONDS)  // make sure we don't wait long!
      .subscribe(                           // now consume!
        n -> logger.info("got {}", n),      // print out the results so far
        t -> logger.error("giving up", t)); // we should see a timeout exception from here

    waitForNewLine();
  }

  private static void waitForNewLine() throws IOException {
    new BufferedReader(new InputStreamReader(System.in)).readLine();
  }
}
