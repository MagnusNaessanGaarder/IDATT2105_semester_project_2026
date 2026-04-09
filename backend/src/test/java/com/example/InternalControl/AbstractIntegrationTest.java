package com.example.InternalControl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using Testcontainers.
 * Provides a shared MySQL container for all integration tests.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class AbstractIntegrationTest {

    static MySQLContainer<?> mysql;

    @BeforeAll
    static void startContainerIfAvailable() {
        // If SPRING_DATASOURCE_URL is set, use external database (e.g., from make dev)
        if (System.getenv("SPRING_DATASOURCE_URL") != null) {
            mysql = null;
            return;
        }

        // Check if Docker is available
        if (!isDockerAvailable()) {
            Assumptions.assumeTrue(false, "Docker is not available; skipping integration tests");
            return;
        }

        // Force using environment variables for Docker
        System.setProperty("testcontainers.docker.client.strategy", "org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy");
        
        // Configure Docker client for Linux
        if (isLinux()) {
            System.setProperty("docker.host", "unix:///var/run/docker.sock");
        }
        
        // Disable ryuk for faster tests
        System.setProperty("testcontainers.ryuk.disabled", "true");
        
        // Set Docker API version
        System.setProperty("api.version", "1.40");

        try {
            mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);
            mysql.start();
        } catch (Exception e) {
            System.err.println("Failed to start MySQL container: " + e.getMessage());
            Assumptions.assumeTrue(false, "Failed to start Docker container: " + e.getMessage());
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mysql != null && mysql.isRunning()) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }
    }

    private static boolean isLinux() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("linux");
    }

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception exception) {
            System.err.println("Docker not available: " + exception.getMessage());
            return false;
        }
    }
}
