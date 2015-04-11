package io.rxd.common.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecordKey extends Domain {
  private final String id;
  private final long version;

  public final static RecordKey EMPTYKEY = new RecordKey("", -1);

  public RecordKey(@JsonProperty("id") String id,
                   @JsonProperty("version") long version) {
    if (id == null)
      throw new IllegalArgumentException("id must not be null");
    this.id = id;
    this.version = version;
  }

  public String getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return id + ":" + version;
  }
}
