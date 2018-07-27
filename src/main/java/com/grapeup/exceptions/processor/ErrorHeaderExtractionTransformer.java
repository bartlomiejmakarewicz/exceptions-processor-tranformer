package com.grapeup.exceptions.processor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

@Component
class ErrorHeaderExtractionTransformer implements GenericTransformer<Message, Message> {

  private final String headerKey;

  ErrorHeaderExtractionTransformer(@Value("${app.routing-header}") String headerKey) {
    this.headerKey = headerKey;
  }

  @Override
  public Message transform(Message message) {
    if (message.getHeaders().containsKey(headerKey)) {
      return message;
    }
    if (message instanceof ErrorMessage) {
      ErrorMessage errorMessage = (ErrorMessage) message;
      String headerValue = getExchangeHeader(errorMessage);
      if (headerValue != null) {
        MessageHeaders headers = new MutableMessageHeaders(errorMessage.getHeaders());
        headers.put(headerKey, headerValue);
        return new ErrorMessage(errorMessage.getPayload(), headers, errorMessage.getOriginalMessage());
      }
    }
    if (message.getPayload() instanceof MessagingException) {
      String headerValue = getExchangeHeader((MessagingException) message.getPayload());
      if (headerValue != null) {
        MessageHeaders headers = new MutableMessageHeaders(message.getHeaders());
        headers.put(headerKey, headerValue);
        return new GenericMessage<>(message.getPayload(), headers);
      }
    }
    throw new RuntimeException("Routing header is not present");
  }

  private String getExchangeHeader(ErrorMessage errorMessage) {
    if (errorMessage.getPayload() instanceof MessagingException) {
      MessagingException messagingException = (MessagingException) errorMessage.getPayload();
      String headerValue = getExchangeHeader(messagingException);
      if (headerValue != null) {
        return headerValue;
      }
    }
    if (errorMessage.getOriginalMessage() != null) {
      if (errorMessage.getOriginalMessage().getHeaders().containsKey(headerKey)) {
        String headerValue = errorMessage.getOriginalMessage().getHeaders().get(headerKey, String.class);
        if (headerValue != null) {
          return headerValue;
        }
      } else if (errorMessage.getOriginalMessage() instanceof ErrorMessage) {
        String headerValue = getExchangeHeader((ErrorMessage) errorMessage.getOriginalMessage());
        if (headerValue != null) {
          return headerValue;
        }
      }
    }
    return null;
  }

  private String getExchangeHeader(MessagingException messagingException) {
    if (messagingException.getFailedMessage() != null
            && messagingException.getFailedMessage().getHeaders().containsKey(headerKey)) {
      String headerValue = messagingException.getFailedMessage().getHeaders().get(headerKey, String.class);
      if (headerValue != null) {
        return headerValue;
      }
    }
    if (messagingException.getCause() instanceof MessagingException) {
      String headerValue = getExchangeHeader((MessagingException) messagingException.getCause());
      if (headerValue != null) {
        return headerValue;
      }
    }
    return null;
  }
}
