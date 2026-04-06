package com.example.InternalControl;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

public abstract class AbstractIntegrationTest {

  static final MySQLContainer<?> mysql;

  static {
    MySQLContainer<?> container = null;
    if (System.getenv("SPRING_DATASOURCE_URL") == null) {
      try {
        container = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
        container.start();
      } catch (Exception e) {
        // Docker not available, will use H2 from properties
        container = null;
      }
    }
    mysql = container;
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    if (mysql != null) {
      registry.add("spring.datasource.url", mysql::getJdbcUrl);
      registry.add("spring.datasource.username", mysql::getUsername);
      registry.add("spring.datasource.password", mysql::getPassword);
    }
  }
}