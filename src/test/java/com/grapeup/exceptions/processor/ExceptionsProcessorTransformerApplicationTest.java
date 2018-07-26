package com.grapeup.exceptions.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExceptionsProcessorTransformerApplicationTest {

  @Value("${app.routing-header}")
  private final String HEADER_KEY = "exchange";

  @Autowired
  private Processor processor;

  @Autowired
  private MessageCollector messageCollector;

  @Test(expected = RuntimeException.class)
  public void whenMessageWithoutRoutingHeaderIsSentToInput_exceptionIsThrown() {
    processor.input().send(new GenericMessage<>("message without routing header"));
  }

  @Test
  public void whenMessageWithRoutingHeaderIsSentToInput_thenProcessItToJson() throws IOException {
    Map<String, Object> headers = Collections.singletonMap(HEADER_KEY, "routing header");
    Message messageSent = new GenericMessage<>("message with routing header", headers);
    processor.input().send(messageSent);

    Message messageReceived = messageCollector.forChannel(processor.output()).poll();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.readValue(messageReceived.getPayload().toString(), messageSent.getPayload().getClass());
  }

  @Test
  public void whenErrorMessageWithRoutingHeaderIsSentToInput_thenProcessItToJson() throws IOException {
    Map<String, Object> headers = Collections.singletonMap(HEADER_KEY, "routing header");
    Message messageSent = new ErrorMessage(new RuntimeException("some messaging exception"), headers);
    processor.input().send(messageSent);

    Message messageReceived = messageCollector.forChannel(processor.output()).poll();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.readValue(messageReceived.getPayload().toString(), messageSent.getPayload().getClass());
  }

  @Test
  public void whenErrorMessageWithoutRoutingHeaderIsSentToInput_thenFindHeaderInOriginalMessage() throws IOException {
    Map<String, Object> headers = Collections.singletonMap(HEADER_KEY, "routing header");
    Message originalMessage = new GenericMessage<>(new String[]{"message with routing header"}, headers);
    headers = Collections.emptyMap();
    Message messageSent = new ErrorMessage(new RuntimeException("some messaging exception"), headers, originalMessage);
    processor.input().send(messageSent);

    Message messageReceived = messageCollector.forChannel(processor.output()).poll();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.readValue(messageReceived.getPayload().toString(), messageSent.getPayload().getClass());
  }
}