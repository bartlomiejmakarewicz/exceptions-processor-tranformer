package com.grapeup.exceptions.processor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

@SpringBootApplication
@EnableBinding(Processor.class)
public class ExceptionsProcessorTransformerApplication {

  @Value("${app.routing-header}")
  private static final String ROUTING_KEY = "exchange";

  public static void main(String[] args) {
    SpringApplication.run(ExceptionsProcessorTransformerApplication.class, args);
  }

  @Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
  public Message<?> transform(Message<?> message) {
    if (message.getHeaders().containsKey(ROUTING_KEY)) {
      return message;
    }
    if (message instanceof ErrorMessage) {
      String exchange = getExchangeHeader((ErrorMessage) message);
      if (exchange != null) {
        MessageHeaders headers = new MutableMessageHeaders(message.getHeaders());
        headers.put("exchange", exchange);
        return new GenericMessage<>("Error occurred!", headers);
      }
    }
    throw new RuntimeException("Routing header is not present");
  }

  String getExchangeHeader(ErrorMessage errorMessage) {
    if (errorMessage.getPayload() instanceof MessagingException) {
      MessagingException messagingException = (MessagingException) errorMessage.getPayload();
      String exchange = getExchangeHeader(messagingException);
      if (exchange != null) {
        return exchange;
      }
    }
    if (errorMessage.getOriginalMessage() != null) {
      if (errorMessage.getOriginalMessage().getHeaders().containsKey(ROUTING_KEY)) {
        return errorMessage.getOriginalMessage().getHeaders().get(ROUTING_KEY, String.class);
      } else if (errorMessage.getOriginalMessage() instanceof ErrorMessage) {
        String exchange = getExchangeHeader((ErrorMessage) errorMessage.getOriginalMessage());
        if (exchange != null) {
          return exchange;
        }
      }
    }
    return null;
  }

  String getExchangeHeader(MessagingException messagingException) {
    if (messagingException.getFailedMessage().getHeaders().containsKey(ROUTING_KEY)) {
      return messagingException.getFailedMessage().getHeaders().get(ROUTING_KEY, String.class);
    }
    if (messagingException.getCause() instanceof MessagingException) {
      String exchange = getExchangeHeader((MessagingException) messagingException.getCause());
      if (exchange != null) {
        return exchange;
      }
    }
    return null;
  }
}
