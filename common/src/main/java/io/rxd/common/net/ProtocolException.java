package io.rxd.common.net;

public class ProtocolException extends Exception {
  public ProtocolException(String s) {
    super(s);
  }

  public ProtocolException(String s, Exception e) {
    super(s, e);
  }
}
