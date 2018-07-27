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
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.routing-header=exchange"
})
public class TransformationProcessorTest {

  @Value("${app.routing-header}")
  private final String HEADER_KEY = "exchange";

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private Processor processor;

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MessageCollector messageCollector;

  private Map<String, Object> headers() {
    return Collections.singletonMap(HEADER_KEY, "routing header");
  }

  @Test(expected = RuntimeException.class)
  public void givenMessageWithoutRoutingHeader_whenMessageProcessed_thenExceptionIsThrown() {
    processor.input().send(new GenericMessage<>("message without routing header"));
  }

  @Test
  public void givenMessageWithRoutingHeader_whenMessageIsProcessed_thenValidMessageIsSentToOutput() throws IOException {
    // given
    Message messageSent = new GenericMessage<>("message with routing header", headers());

    // when
    processor.input().send(messageSent);

    // then
    Message messageReceived = messageCollector.forChannel(processor.output()).poll();
    assertNotNull(messageReceived);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.readValue(messageReceived.getPayload().toString(), messageSent.getPayload().getClass());
  }

  @Test
  public void givenErrorMessageWithRoutingHeader_whenMessageIsProcessed_thenValidMessageIsSentToOutput() throws IOException {
    // given
    Message messageSent = new ErrorMessage(new RuntimeException("some messaging exception"), headers());

    // when
    processor.input().send(messageSent);

    // then
    Message messageReceived = messageCollector.forChannel(processor.output()).poll();
    assertNotNull(messageReceived);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.readValue(messageReceived.getPayload().toString(), messageSent.getPayload().getClass());
  }

  @Test
  public void givenErrorMessageWithOriginalMessage_whenMessageIsProcessed_thenValidMessageIsSentToOutput() throws IOException {
    // given
    Message originalMessage = new GenericMessage<>("message with routing header", headers());
    Message messageSent = new ErrorMessage(new RuntimeException("some messaging exception"), Collections.emptyMap(), originalMessage);

    // when
    processor.input().send(messageSent);

    // then
    Message messageReceived = messageCollector.forChannel(processor.output()).poll();
    assertNotNull(messageReceived);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.readValue(messageReceived.getPayload().toString(), messageSent.getPayload().getClass());
  }
}