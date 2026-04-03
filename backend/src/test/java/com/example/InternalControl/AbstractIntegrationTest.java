package com.example.InternalControl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * Base class for integration tests with Testcontainers.
 * Skips tests if Docker is not available.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(AbstractIntegrationTest.DockerAvailableCondition.class)
public abstract class AbstractIntegrationTest {

  static MySQLContainer<?> mysql;
  static boolean dockerAvailable = false;

  @BeforeAll
  static void setup() {
    try {
      DockerClientFactory.instance().client();
      dockerAvailable = true;
      
      if (mysql == null) {
        mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
        mysql.start();
      }
    } catch (Exception e) {
      dockerAvailable = false;
      mysql = null;
      // Docker not available, integration tests will be skipped
    }
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    if (mysql != null && mysql.isRunning()) {
      registry.add("spring.datasource.url", mysql::getJdbcUrl);
      registry.add("spring.datasource.username", mysql::getUsername);
      registry.add("spring.datasource.password", mysql::getPassword);
      registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
      registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }
  }

  /**
   * JUnit condition that disables tests when Docker is not available
   */
  public static class DockerAvailableCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
      // Check if Docker is available
      try {
        DockerClientFactory.instance().client();
        return ConditionEvaluationResult.enabled("Docker is available");
      } catch (Exception e) {
        return ConditionEvaluationResult.disabled("Docker is not available - skipping integration test");
      }
    }
  }
}
