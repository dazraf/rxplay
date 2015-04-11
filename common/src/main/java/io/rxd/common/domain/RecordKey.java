package io.rxd.common.domain;

public class RecordKey extends Domain {
  private final String id;
  private final long version;

  public final static RecordKey EMPTYKEY = new RecordKey("", -1);

  public RecordKey(String id, long version) {
    this.id = id;
    this.version = version;
  }

  public String getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }
}
