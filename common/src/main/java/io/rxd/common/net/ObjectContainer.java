package io.rxd.common.net;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ObjectContainer<T> {
  private T contained;

  @JsonCreator
  public ObjectContainer(@JsonProperty("contained") T contained) {
    this.contained = contained;
  }

  public T getContained() {
    return contained;
  }
}
