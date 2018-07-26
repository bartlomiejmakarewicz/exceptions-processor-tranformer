package com.grapeup.exceptions.processor;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(Processor.class)
class TransformationProcessor {

  private final ErrorHeaderExtractionTransformer errorHeaderExtractionTransformer;

  TransformationProcessor(ErrorHeaderExtractionTransformer errorHeaderExtractionTransformer) {
    this.errorHeaderExtractionTransformer = errorHeaderExtractionTransformer;
  }

  @Bean
  IntegrationFlow transformationFlow() {
    return IntegrationFlows.from(Processor.INPUT)
            .transform(errorHeaderExtractionTransformer)
            .transform(Transformers.toJson())
            .channel(Processor.OUTPUT)
            .get();
  }
}
