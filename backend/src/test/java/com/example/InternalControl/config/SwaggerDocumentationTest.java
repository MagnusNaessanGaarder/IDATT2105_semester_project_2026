package com.example.InternalControl.config;

import com.example.InternalControl.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Swagger Documentation Coverage Tests")
class SwaggerDocumentationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode openApiJson;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        openApiJson = objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Set<String> pathKeys() {
        JsonNode pathsNode = openApiJson.path("paths");
        Set<String> keys = new HashSet<>();
        Iterator<String> fields = pathsNode.fieldNames();
        while (fields.hasNext()) {
            keys.add(fields.next());
        }
        return keys;
    }

    @Test
    @DisplayName("Should expose Swagger UI endpoint")
    void shouldExposeSwaggerUiEndpoint() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should expose OpenAPI JSON endpoint")
    void shouldExposeOpenApiJsonEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should have documented endpoints with no missing summaries or responses")
    void shouldHaveDocumentedEndpointsWithNoMissingSummariesOrResponses() {
        JsonNode paths = openApiJson.path("paths");
        assertThat(paths.isObject()).isTrue();
        assertThat(paths.size()).isGreaterThan(0);

        int operationCount = 0;
        int missingSummary = 0;
        int missingResponses = 0;

        Iterator<String> pathIterator = paths.fieldNames();
        while (pathIterator.hasNext()) {
            JsonNode operations = paths.get(pathIterator.next());
            Iterator<String> methods = operations.fieldNames();
            while (methods.hasNext()) {
                String method = methods.next();
                if ("parameters".equals(method)) {
                    continue;
                }
                operationCount++;
                JsonNode operation = operations.get(method);
                String summary = operation.path("summary").asText("");
                if (summary.isBlank()) {
                    missingSummary++;
                }
                if (operation.path("responses").isMissingNode() || operation.path("responses").size() == 0) {
                    missingResponses++;
                }
            }
        }

        assertThat(operationCount).isGreaterThanOrEqualTo(1);
        assertThat(missingSummary).isZero();
        assertThat(missingResponses).isZero();
    }

    @Test
    @DisplayName("Should document authentication endpoints")
    void shouldDocumentAuthenticationEndpoints() {
        Set<String> paths = pathKeys();
        assertThat(paths).contains("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh");
    }

    @Test
    @DisplayName("Should document user management endpoints")
    void shouldDocumentUserManagementEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/users"))).isTrue();
    }

    @Test
    @DisplayName("Should document checklist endpoints")
    void shouldDocumentChecklistEndpoints() {
        Set<String> paths = pathKeys();
        assertThat(paths.stream().anyMatch(path -> path.startsWith("/api/v1/checklists/templates"))).isTrue();
        assertThat(paths.stream().anyMatch(path -> path.startsWith("/api/v1/checklists/runs"))).isTrue();
    }

    @Test
    @DisplayName("Should document deviation report endpoints")
    void shouldDocumentDeviationReportEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/deviations"))).isTrue();
    }

    @Test
    @DisplayName("Should document temperature logging endpoints")
    void shouldDocumentTemperatureLoggingEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/temperature"))).isTrue();
    }

    @Test
    @DisplayName("Should document notification endpoints")
    void shouldDocumentNotificationEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/notifications"))).isTrue();
    }

    @Test
    @DisplayName("Should document file management endpoints")
    void shouldDocumentFileManagementEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/files"))).isTrue();
    }

    @Test
    @DisplayName("Should document analytics endpoints")
    void shouldDocumentAnalyticsEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/analytics"))).isTrue();
    }

    @Test
    @DisplayName("Should document export endpoints")
    void shouldDocumentExportEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/exports"))).isTrue();
    }

    @Test
    @DisplayName("Should document training record endpoints")
    void shouldDocumentTrainingRecordEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/training"))).isTrue();
    }

    @Test
    @DisplayName("Should document location endpoints")
    void shouldDocumentLocationEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/locations"))).isTrue();
    }

    @Test
    @DisplayName("Should document admin audit log endpoints")
    void shouldDocumentAdminAuditLogEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/admin/audit-log"))).isTrue();
    }

    @Test
    @DisplayName("Should document role and permission endpoints")
    void shouldDocumentRoleAndPermissionEndpoints() {
        Set<String> paths = pathKeys();
        assertThat(paths.stream().anyMatch(path -> path.startsWith("/api/admin/roles"))).isTrue();
        assertThat(paths.stream().anyMatch(path -> path.startsWith("/api/admin/permissions"))).isTrue();
    }

    @Test
    @DisplayName("Should document organization settings endpoints")
    void shouldDocumentOrganizationSettingsEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/organizations/") && path.contains("/settings"))).isTrue();
    }

    @Test
    @DisplayName("Should document identity provider endpoints")
    void shouldDocumentIdentityProviderEndpoints() {
        assertThat(pathKeys().stream().anyMatch(path -> path.startsWith("/api/v1/identity"))).isTrue();
    }

    @Test
    @DisplayName("All documented paths should have defined operations")
    void allDocumentedPathsShouldHaveDefinedOperations() {
        JsonNode paths = openApiJson.path("paths");
        Iterator<String> pathIterator = paths.fieldNames();
        while (pathIterator.hasNext()) {
            JsonNode operations = paths.get(pathIterator.next());
            int operationCount = 0;
            Iterator<String> methods = operations.fieldNames();
            while (methods.hasNext()) {
                String method = methods.next();
                if (!"parameters".equals(method)) {
                    operationCount++;
                }
            }
            assertThat(operationCount).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Should have security scheme configured for protected endpoints")
    void shouldHaveSecuritySchemeConfiguredForProtectedEndpoints() {
        JsonNode securitySchemes = openApiJson.path("components").path("securitySchemes");
        assertThat(securitySchemes.isObject()).isTrue();
        assertThat(securitySchemes.has("bearerAuth")).isTrue();
    }
}
