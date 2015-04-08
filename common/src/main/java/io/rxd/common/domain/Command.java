package io.rxd.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.util.UUID;

public class Command extends Domain {
  private final static UUID EMPTY_UUID = new UUID(0, 0);
  private String sessionId = "";

  @JsonIgnore // this is serialised by our framework efficiently
  private UUID id = EMPTY_UUID;

  @JsonIgnore
  private Subject<Document, Document> incoming = PublishSubject.create();

  @JsonIgnore
  private Subject<Document, Document> outgoing = PublishSubject.create();

  public Observable<Document> incoming() {
    return incoming;
  }

  public Observer<Document> outgoing() {
    return outgoing;
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

  public Observer<Document> incomingObserver() {
    return incoming;
  }

  public Observable<Document> outgoingObservable() {
    return outgoing;
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
