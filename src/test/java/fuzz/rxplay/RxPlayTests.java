package fuzz.rxplay;

import fuzz.rxplay.util.Factory0;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class RxPlayTests {
  private static final Logger logger = LoggerFactory.getLogger(RxPlayTests.class);

  @Test
  public void handlingFlakyConnectionsAndSlowProducersTest () throws Throwable {
    AtomicInteger receivedResult = new AtomicInteger();
    ArrayList<Throwable> exceptionsReceived = new ArrayList<>();

    // a connection factory to simulate something like netty
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
        receivedResult::addAndGet,          // collect how many results we got
        exceptionsReceived::add);           // we should see a timeout exception from here

    assertEquals(receivedResult.get(), 3);
    assertEquals(exceptionsReceived.size(), 1);
    Throwable theException = exceptionsReceived.get(0);
    assertTrue(theException instanceof TimeoutException);
  }
}
