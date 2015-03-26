package fuzz.rxplay.intf;

public class QueryExpression {
  private String database;
  private String collection;
  private String query;

  public QueryExpression() {
  }


  public String getDatabase() {
    return database;
  }

  public String getCollection() {
    return collection;
  }

  public String getQuery() {
    return query;
  }

  public QueryExpression withDatabase() {
    return null; // TODO
  }
}
