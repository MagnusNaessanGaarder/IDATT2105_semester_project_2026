package com.example.InternalControl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

/**
 * Configuration for Jackson ObjectMapper.
 * Configures serialization/deserialization settings including date/time handling and Hibernate support.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Configuration
public class JacksonConfig {

  /**
   * Creates the primary ObjectMapper bean with date/time support.
   */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Hibernate6Module hibernate6Module = new Hibernate6Module();
    hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
    mapper.registerModule(hibernate6Module);

    return mapper;
  }
}
