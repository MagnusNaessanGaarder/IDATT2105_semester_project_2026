package com.example.InternalControl;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using Testcontainers.
 * Provides a shared MySQL container for all integration tests.
 *
 * @author TriTacLe
 * @since 1.0
 */
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class AbstractIntegrationTest {

    private static final MySQLContainer<?> mysql;

    static {
        // Configure Testcontainers to use the Docker socket
        System.setProperty("docker.host", "unix:///var/run/docker.sock");
        System.setProperty("testcontainers.docker.socket.override", "/var/run/docker.sock");
        System.setProperty("testcontainers.ryuk.disabled", "true");
        
        if (System.getenv("SPRING_DATASOURCE_URL") != null) {
            // Use external database if configured
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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mysql != null) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        }
    }
}