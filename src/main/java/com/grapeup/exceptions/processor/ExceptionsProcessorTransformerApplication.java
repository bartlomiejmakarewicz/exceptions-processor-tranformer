package com.grapeup.exceptions.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageProcessorSpec;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableBinding(Processor.class)
@Slf4j
public class ExceptionsProcessorTransformerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExceptionsProcessorTransformerApplication.class, args);
  }

  @Bean
  DirectChannel logChannel() {
    return new DirectChannel();
  }

  @Autowired
  GenericTransformer<?, ?> transformer;

  @Bean
  IntegrationFlow logFlow() {
    return IntegrationFlows.from(Processor.INPUT)
            .log()
            .transform(transformer)
            .transform(Transformers.toJson())
            .log()
            .channel(Processor.OUTPUT)
            .get();
  }
}

@Component
class CustomTransformer implements GenericTransformer<Message, Message> {

  @Value("${app.routing-header}")
  private static final String ROUTING_KEY = "exchange";

  @Override
  public Message transform(Message message) {
    if (message.getHeaders().containsKey(ROUTING_KEY)) {
      return message;
    }
    if (message instanceof ErrorMessage) {
      String exchange = getExchangeHeader((ErrorMessage) message);
      if (exchange != null) {
        MessageHeaders headers = new MutableMessageHeaders(message.getHeaders());
        headers.put(ROUTING_KEY, exchange);
        return new GenericMessage<>("Error occurred!", headers);
      }
    }
    throw new RuntimeException("Routing header is not present");
  }

  private String getExchangeHeader(ErrorMessage errorMessage) {
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

  private String getExchangeHeader(MessagingException messagingException) {
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
