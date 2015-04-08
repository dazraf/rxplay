package io.rxd.common.domain;

public class UpsertAllCommand extends Command {
  private String database;
  private String collection;

  private UpsertAllCommand() {

  }

  public UpsertAllCommand(String database, String collection) {
    this.database = database;
    this.collection = collection;
  }

  public String getDatabase() {
    return database;
  }

  public String getCollection() {
    return collection;
  }

  @Override
  public String toString() {
    return super.toString() + "-" + database + "-" + collection;
  }
}
