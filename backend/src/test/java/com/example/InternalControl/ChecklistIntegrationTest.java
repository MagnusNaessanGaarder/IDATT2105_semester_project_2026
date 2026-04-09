package com.example.InternalControl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Checklist API using Java HttpClient.
 * Tests the full workflow from template creation to run completion.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

@Import(TestBlobConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChecklistIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    private ObjectMapper objectMapper;
    private HttpClient client;
    private String authToken;
    private static final Integer ORG_NUMBER = 937219997;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        // Register and login to get auth token
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "checklist_test_" + timestamp + "@example.com";
        String password = "TestPass123!";

        // Register
        String registerJson = String.format(
            "{\"fullName\": \"Checklist Test User\", \"email\": \"%s\", \"password\": \"%s\"}",
            email, password
        );

        HttpRequest registerRequest = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerJson))
            .build();

        HttpResponse<String> registerResponse = client.send(registerRequest,
            HttpResponse.BodyHandlers.ofString());

        assertThat(registerResponse.statusCode()).isIn(200, 201);

        JsonNode registerBody = objectMapper.readTree(registerResponse.body());
        authToken = registerBody.get("accessToken").asText();
    }

    private String getBaseUrl() {
        String port = environment.getProperty("local.server.port", "8080");
        return "http://localhost:" + port + "/api";
    }

    @Test
    void shouldCreateAndRetrieveTemplate() throws Exception {
        // Create template
        String templateJson = "{" +
            "\"title\": \"Daily Temperature Check\"," +
            "\"description\": \"Check all fridge temperatures\"," +
            "\"moduleType\": \"FOOD\"," +
            "\"frequency\": \"DAILY\"" +
            "}";

        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/checklists/templates?orgNumber=" + ORG_NUMBER))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + authToken)
            .POST(HttpRequest.BodyPublishers.ofString(templateJson))
            .build();

        HttpResponse<String> createResponse = client.send(createRequest,
            HttpResponse.BodyHandlers.ofString());

        assertThat(createResponse.statusCode()).isEqualTo(201);

        JsonNode createdTemplate = objectMapper.readTree(createResponse.body());
        Long templateId = createdTemplate.get("templateId").asLong();
        assertThat(templateId).isPositive();
        assertThat(createdTemplate.get("title").asText()).isEqualTo("Daily Temperature Check");

        // Retrieve template
        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/checklists/templates/" + templateId + "?orgNumber=" + ORG_NUMBER))
            .header("Authorization", "Bearer " + authToken)
            .GET()
            .build();

        HttpResponse<String> getResponse = client.send(getRequest,
            HttpResponse.BodyHandlers.ofString());

        assertThat(getResponse.statusCode()).isEqualTo(200);

        JsonNode retrievedTemplate = objectMapper.readTree(getResponse.body());
        assertThat(retrievedTemplate.get("templateId").asLong()).isEqualTo(templateId);
        assertThat(retrievedTemplate.get("title").asText()).isEqualTo("Daily Temperature Check");
    }

    @Test
    void shouldGetActiveTemplates() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/checklists/templates/active?orgNumber=" + ORG_NUMBER))
            .header("Authorization", "Bearer " + authToken)
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode templates = objectMapper.readTree(response.body());
        assertThat(templates.isArray()).isTrue();
    }

    @Test
    void shouldGetTemplatesByModule() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/checklists/templates/module/FOOD?orgNumber=" + ORG_NUMBER))
            .header("Authorization", "Bearer " + authToken)
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode templates = objectMapper.readTree(response.body());
        assertThat(templates.isArray()).isTrue();
    }

    @Test
    void shouldReturn401WhenNoAuthToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getBaseUrl() + "/checklists/templates?orgNumber=" + ORG_NUMBER))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
