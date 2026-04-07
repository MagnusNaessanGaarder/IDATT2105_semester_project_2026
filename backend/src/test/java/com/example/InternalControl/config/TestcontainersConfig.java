package com.example.InternalControl.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Global TestContainers configuration for all tests.
 * This ensures every test gets a database without extending a base class.
 */
public class TestcontainersConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final MySQLContainer<?> mysql;

    static {
        if (System.getenv("SPRING_DATASOURCE_URL") != null) {
            mysql = null;
        } else {
            mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);
            mysql.start();
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        if (mysql != null) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "spring.datasource.url=" + mysql.getJdbcUrl(),
                    "spring.datasource.username=" + mysql.getUsername(),
                    "spring.datasource.password=" + mysql.getPassword(),
                    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
            );
        }
    }
}
