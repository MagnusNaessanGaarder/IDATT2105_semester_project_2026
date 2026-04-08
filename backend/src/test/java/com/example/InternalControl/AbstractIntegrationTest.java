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
        if (System.getenv("SPRING_DATASOURCE_URL") != null) {
            mysql = null;
            return;
        }

        if (!isDockerAvailable()) {
            Assumptions.assumeTrue(false, "Docker is not available; skipping integration tests");
            return;
        }

        if (isLinux()) {
            setPropertyIfAbsent("docker.host", "unix:///var/run/docker.sock");
            setPropertyIfAbsent("testcontainers.docker.socket.override", "/var/run/docker.sock");
        }
        setPropertyIfAbsent("testcontainers.ryuk.disabled", "true");

        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        mysql.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mysql != null) {
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
            return false;
        }
    }

    private static void setPropertyIfAbsent(String propertyName, String value) {
        if (System.getProperty(propertyName) != null) {
            return;
        }
        String envVarName = propertyName.toUpperCase().replace('.', '_');
        if (System.getenv(envVarName) != null) {
            return;
        }
        System.setProperty(propertyName, value);
    }
}
