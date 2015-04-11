package io.rxd.common.domain;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.bson.BSONObject;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public class Document extends Domain implements BSONObject {
  private DBObject dbObject;
  private final static String KEYFIELD = "__rxd_key";

  public Document(DBObject dbObject) {
    if (dbObject == null)
      throw new IllegalArgumentException("dbObject cannot be null");
    this.dbObject = dbObject;
  }

  public static Document parse(String text) throws ParseException {
    Object obj = JSON.parse(text);
    if (obj instanceof DBObject)
      return new Document((DBObject) obj);
    else
      throw new ParseException("expected a JSON object/array", 0);
  }

  public static Document from(DBObject object) {
    return new Document(object);
  }

  @Override
  public Object put(String key, Object v) {
    return dbObject.put(key, v);
  }

  @Override
  public void putAll(BSONObject o) {
    dbObject.putAll(o);
  }

  @Override
  public void putAll(Map m) {
    dbObject.putAll(m);
  }

  @Override
  public Object get(String key) {
    return dbObject.get(key);
  }

  @Override
  public Map toMap() {
    return dbObject.toMap();
  }

  @Override
  public Object removeField(String key) {
    return dbObject.removeField(key);
  }

  @Override
  public boolean containsKey(String key) {
    return dbObject.containsKey(key);
  }

  @Override
  public boolean containsField(String s) {
    return dbObject.containsField(s);
  }

  @Override
  public Set<String> keySet() {
    return dbObject.keySet();
  }

  @Override
  public String toString() {
    return dbObject.toString();
  }

  @Override
  public int hashCode() {
    return dbObject.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj != null && obj.toString().equals(this.toString()));
  }

  public RecordKey getKey() {
    if (dbObject.containsField(KEYFIELD)) {
      DBObject key = (DBObject) dbObject.get(KEYFIELD);
      return new RecordKey((String)key.get("id"), (long)key.get("version"));
    } else {
      return RecordKey.EMPTYKEY;
    }
  }

  public Document withKey(RecordKey key) {
    if (!dbObject.containsField(KEYFIELD)) {
      dbObject.put(KEYFIELD, new BasicDBObject());
    }
    DBObject dbKey = (DBObject)dbObject.get(KEYFIELD);
    dbKey.put("id", key.getId());
    dbKey.put("version", key.getVersion());
    return this;
  }
}
