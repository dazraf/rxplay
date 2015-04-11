package io.rxd.common.kafka.utils;

import kafka.utils.Time;

public class SystemTime implements Time {
  public long milliseconds() {
    return System.currentTimeMillis();
  }

  public long nanoseconds() {
    return System.nanoTime();
  }

  public void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      // Ignore
    }
  }
}