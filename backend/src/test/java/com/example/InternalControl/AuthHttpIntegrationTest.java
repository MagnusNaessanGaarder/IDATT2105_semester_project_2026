package com.example.InternalControl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using Java 11+ HttpClient.
 * Requires the server to be running separately.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(
    locations = "classpath:application-test.yml",
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    }
)
@Import(TestBlobConfig.class)
class AuthHttpIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    private ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String getBaseUrl() {
        String port = environment.getProperty("local.server.port", "8080");
        return "http://localhost:" + port + "/api";
    }

    @Test
    void contextLoads() {
        // Verifies Spring context loads
        assertThat(environment).isNotNull();
    }

    @Test
    void testRegistrationAndLoginFlow() throws Exception {
        // This test only runs if server is manually started
        // Skip if server not available
        if (!isServerRunning()) {
            return;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "http_test_" + timestamp + "@example.com";
        String password = "TestPass123!";
        String fullName = "HTTP Test User";

        // Register
        String registerJson = String.format(
            "{\"fullName\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}",
            fullName, email, password
        );

        HttpRequest registerRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(registerJson))
                .build();

        HttpResponse<String> registerResponse = client.send(registerRequest, 
                HttpResponse.BodyHandlers.ofString());

        assertThat(registerResponse.statusCode())
            .withFailMessage("Registration failed: " + registerResponse.body())
            .isIn(200, 201);

        JsonNode registerBody = objectMapper.readTree(registerResponse.body());
        assertThat(registerBody.has("accessToken")).isTrue();

        // Login
        String loginJson = String.format(
            "{\"email\": \"%s\", \"password\": \"%s\"}",
            email, password
        );

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, 
                HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode())
            .withFailMessage("Login failed: " + loginResponse.body())
            .isEqualTo(200);

        JsonNode loginBody = objectMapper.readTree(loginResponse.body());
        assertThat(loginBody.has("accessToken")).isTrue();
        assertThat(loginBody.get("email").asText()).isEqualTo(email);
    }

    private boolean isServerRunning() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl().replace("/api", "") + "/actuator/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
