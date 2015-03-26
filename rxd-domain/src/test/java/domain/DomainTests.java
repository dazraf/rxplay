package domain;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import org.junit.Test;
import rxplay.domain.Domain;
import rxplay.domain.Domain.CreateRequestChannel;

import java.io.FileOutputStream;
import java.util.UUID;

public class DomainTests {
  @Test
  public void test() {
    UUID uuid = UUID.randomUUID();
    String suuid = uuid.toString();
    System.out.println(suuid.length());
    CreateRequestChannel request = Domain.CreateRequestChannel.newBuilder()
      .setCommandId(UUID.randomUUID().toString())
      .setCommandType("some type")
      .setPayload(ByteString.copyFrom("hello".getBytes()))
      .build();
    // request.writeTo();
//    ByteBuffer buffer = request.getPayload().asReadOnlyByteBuffer();
////    Domain.createRequestChannelOrBuilder
  }
}
