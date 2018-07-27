package com.grapeup.exceptions.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class ErrorHeaderExtractionTransformerTest {

  private final String headerKey = "exchange";

  private final ErrorHeaderExtractionTransformer transformer;

  private Map<String, Object> headers() {
    return Collections.singletonMap(headerKey, "routing header");
  }

  public ErrorHeaderExtractionTransformerTest() {
    transformer = new ErrorHeaderExtractionTransformer(headerKey);
  }

  @Test(expected = RuntimeException.class)
  public void givenMessageWithoutRoutingHeader_whenPassedToTransformer_thenExceptionIsThrown() {
    // given
    Message<String> message = new GenericMessage<>("message without routing header");

    // when
    transformer.transform(message);

    // then
    // RuntimeException is thrown
  }

  @Test
  public void givenMessageWithRoutingHeader_whenPassedToTransformer_thenValidMessageIsReturned() {
    // given
    Message<String> message = new GenericMessage<>("message with routing header", headers());

    // when
    Message messageTransformed = transformer.transform(message);

    // then
    assertThat(messageTransformed.getPayload(), is(message.getPayload()));
    assertThat(messageTransformed.getHeaders().get(headerKey), is(headers().get(headerKey)));
  }

  @Test
  public void givenErrorMessageWithOriginalMessage_whenPassedToTransformer_thenValidMessageIsReturned() {
    // given
    Message<String> originalMessage = new GenericMessage<>("message with routing header", headers());
    Message message = new ErrorMessage(new RuntimeException("runtime exception"), Collections.emptyMap(), originalMessage);

    // when
    Message messageTransformed = transformer.transform(message);

    // then
    assertThat(messageTransformed.getPayload(), is(message.getPayload()));
    assertThat(messageTransformed.getHeaders().get(headerKey), is(headers().get(headerKey)));
  }

  @Test
  public void givenErrorMessageWithFailedMessage_whenPassedToTransformer_thenValidMessageIsReturned() {
    // given
    Message<String> originalMessage = new GenericMessage<>("message with routing header", headers());
    Message message = new ErrorMessage(new MessagingException(originalMessage, "messaging exception"), Collections.emptyMap());

    // when
    Message messageTransformed = transformer.transform(message);

    // then
    assertThat(messageTransformed.getPayload(), is(message.getPayload()));
    assertThat(messageTransformed.getHeaders().get(headerKey), is(headers().get(headerKey)));
  }

  @Test
  public void givenErrorMessageWithOriginalMessageNested_whenPassedToTransformer_thenValidMessageIsReturned() {
    // given
    Message<String> originalMessage = new GenericMessage<>("message with routing header", headers());
    Message messageWrapped = new ErrorMessage(new RuntimeException("runtime exception"), Collections.emptyMap(), originalMessage);
    Message message = new ErrorMessage(new RuntimeException("runtime exception"), Collections.emptyMap(), messageWrapped);

    // when
    Message messageTransformed = transformer.transform(message);

    // then
    assertThat(messageTransformed.getPayload(), is(message.getPayload()));
    assertThat(messageTransformed.getHeaders().get(headerKey), is(headers().get(headerKey)));
  }

  @Test
  public void givenErrorMessageWithFailedMessageNested_whenPassedToTransformer_thenValidMessageIsReturned() {
    // given
    Message<String> originalMessage = new GenericMessage<>("message with routing header", headers());
    Exception exceptionWrapped = new MessagingException(originalMessage, "messaging exception");
    Message message = new ErrorMessage(new MessagingException("wrapping exception", exceptionWrapped), Collections.emptyMap());

    // when
    Message messageTransformed = transformer.transform(message);

    // then
    assertThat(messageTransformed.getPayload(), is(message.getPayload()));
    assertThat(messageTransformed.getHeaders().get(headerKey), is(headers().get(headerKey)));
  }
}