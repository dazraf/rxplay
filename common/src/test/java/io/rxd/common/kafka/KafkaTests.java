package io.rxd.common.kafka;

import io.rxd.common.kafka.utils.EmbeddedKafkaCluster;
import io.rxd.common.kafka.utils.EmbeddedZookeeper;
import io.rxd.common.kafka.utils.TestUtils;
import kafka.server.KafkaServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class KafkaTests {
  private static final String DEFAULT_LOG_DIR = "/tmp/embedded/kafka/";
  public static final String LOCALHOST_BROKER = "0:localhost:9092";
  public static final String TEST_TOPIC = "test-topic";

  private KafkaServer kafkaServer;
  private EmbeddedZookeeper embeddedZookeeper;
  private EmbeddedKafkaCluster embeddedKafkaCluster;

  @Before
  public void before() throws IOException {
    embeddedZookeeper = new EmbeddedZookeeper(2181);
    embeddedZookeeper.startup();
    embeddedKafkaCluster = new EmbeddedKafkaCluster(embeddedZookeeper.getConnection(), new Properties());
    embeddedKafkaCluster.startup();
  }

  @After
  public void after() {
    embeddedKafkaCluster.shutdown();
    embeddedZookeeper.shutdown();
  }

  @Test
  public void simpleKafkaTest() {
  }

  private Properties getBrokerProperties() {
    Properties properties = new Properties();
    File logDir = TestUtils.constructTempDir("kafka-local");
    properties.put("port", 9092 + "");
    properties.put("broker.id", 1 + "");
    properties.put("enable.zookeeper", "false");
    properties.put("zookeeper.connect", "false");
    properties.setProperty("log.dir", logDir.getAbsolutePath());
    properties.setProperty("log.flush.interval.messages", String.valueOf(1));
    return properties;
  }


}
