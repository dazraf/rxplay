package io.rxd.common.domain;

import io.rxd.common.domain.Domain;

public class Person extends Domain {
  private String name;

  private Person() {

  }

  public Person(String name) {
    this.name = name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
