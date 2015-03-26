package fuzz.rxplay.intf;

import com.sun.prism.impl.Disposer;
import rx.Observable;

public interface Intf {
  Observable<Disposer.Record> query(QueryExpression query);
}
