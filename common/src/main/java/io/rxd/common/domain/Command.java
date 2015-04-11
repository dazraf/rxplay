package io.rxd.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.util.UUID;

public class Command<TParameters extends Domain, TResults extends Domain> extends Domain {
  private final static UUID EMPTY_UUID = new UUID(0, 0);
  private String sessionId = "";

  @JsonIgnore // this is serialised by our framework efficiently
  private UUID id = EMPTY_UUID;

  @JsonIgnore
  private Subject<TResults, TResults> results = PublishSubject.create();

  @JsonIgnore
  private Subject<TParameters, TParameters> parameters = PublishSubject.create();

  public Observable<TResults> results() {
    return results;
  }

  public Observer<TParameters> parameters() {
    return parameters;
  }

  /// INTERNALS - MAKE PACKAGE ONLY
  public Command prepare() {
    if (id == EMPTY_UUID)
      id = UUID.randomUUID();
    return this;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public Observer<TResults> resultsObserver() {
    return results;
  }

  public Observable<TParameters> parametersObservable() {
    return parameters;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public String toString() {
    return "command-" + sessionId + "-" + id.toString();
  }
}
