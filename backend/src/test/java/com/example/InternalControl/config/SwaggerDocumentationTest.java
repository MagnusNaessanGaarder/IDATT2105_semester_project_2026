package com.example.InternalControl.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for verifying Swagger/OpenAPI documentation coverage.
 * Ensures all REST endpoints have proper Swagger annotations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-test.properties",
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@DisplayName("Swagger Documentation Coverage Tests")
class SwaggerDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenAPI openAPI;

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
    @DisplayName("Should have at least 100 documented endpoints")
    void shouldHaveAtLeast100DocumentedEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        long endpointCount = paths.values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .count();

        assertThat(endpointCount)
                .withFailMessage("Expected at least 100 documented endpoints, but found %d", endpointCount)
                .isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("All endpoints should have operation summaries")
    void allEndpointsShouldHaveOperationSummaries() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        long operationsWithoutSummary = paths.entrySet().stream()
                .flatMap(entry -> entry.getValue().readOperations().stream()
                        .map(op -> Map.entry(entry.getKey(), op)))
                .filter(entry -> {
                    Operation op = entry.getValue();
                    return op.getSummary() == null || op.getSummary().isBlank();
                })
                .count();

        assertThat(operationsWithoutSummary)
                .withFailMessage("Found %d endpoints without operation summaries", operationsWithoutSummary)
                .isZero();
    }

    @Test
    @DisplayName("All endpoints should have at least one response documented")
    void allEndpointsShouldHaveAtLeastOneResponseDocumented() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        long operationsWithoutResponses = paths.entrySet().stream()
                .flatMap(entry -> entry.getValue().readOperations().stream()
                        .map(op -> Map.entry(entry.getKey(), op)))
                .filter(entry -> {
                    Operation op = entry.getValue();
                    return op.getResponses() == null || op.getResponses().isEmpty();
                })
                .count();

        assertThat(operationsWithoutResponses)
                .withFailMessage("Found %d endpoints without documented responses", operationsWithoutResponses)
                .isZero();
    }

    @Test
    @DisplayName("Should document authentication endpoints")
    void shouldDocumentAuthenticationEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        // Check for auth endpoints
        assertThat(paths.containsKey("/api/v1/auth/login"))
                .withFailMessage("Login endpoint should be documented")
                .isTrue();
        assertThat(paths.containsKey("/api/v1/auth/register"))
                .withFailMessage("Register endpoint should be documented")
                .isTrue();
        assertThat(paths.containsKey("/api/v1/auth/refresh"))
                .withFailMessage("Refresh token endpoint should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document user management endpoints")
    void shouldDocumentUserManagementEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        // Check for user endpoints (using pattern matching since some have path variables)
        boolean hasUsersEndpoint = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/users"));

        assertThat(hasUsersEndpoint)
                .withFailMessage("User management endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document checklist endpoints")
    void shouldDocumentChecklistEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        // Check for checklist endpoints
        boolean hasChecklistTemplates = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/checklists/templates"));

        boolean hasChecklistRuns = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/checklists/runs"));

        assertThat(hasChecklistTemplates)
                .withFailMessage("Checklist template endpoints should be documented")
                .isTrue();

        assertThat(hasChecklistRuns)
                .withFailMessage("Checklist run endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document deviation report endpoints")
    void shouldDocumentDeviationReportEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasDeviationEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/deviations"));

        assertThat(hasDeviationEndpoints)
                .withFailMessage("Deviation report endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document temperature logging endpoints")
    void shouldDocumentTemperatureLoggingEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasTemperatureEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/temperature"));

        assertThat(hasTemperatureEndpoints)
                .withFailMessage("Temperature logging endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document notification endpoints")
    void shouldDocumentNotificationEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasNotificationEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/notifications"));

        assertThat(hasNotificationEndpoints)
                .withFailMessage("Notification endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document file management endpoints")
    void shouldDocumentFileManagementEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasFileEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/files"));

        assertThat(hasFileEndpoints)
                .withFailMessage("File management endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document analytics endpoints")
    void shouldDocumentAnalyticsEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasAnalyticsEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/analytics"));

        assertThat(hasAnalyticsEndpoints)
                .withFailMessage("Analytics endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document export endpoints")
    void shouldDocumentExportEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasExportEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/exports"));

        assertThat(hasExportEndpoints)
                .withFailMessage("Export endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document training record endpoints")
    void shouldDocumentTrainingRecordEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasTrainingEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/training"));

        assertThat(hasTrainingEndpoints)
                .withFailMessage("Training record endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document location endpoints")
    void shouldDocumentLocationEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasLocationEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/locations"));

        assertThat(hasLocationEndpoints)
                .withFailMessage("Location endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document admin audit log endpoints")
    void shouldDocumentAdminAuditLogEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasAuditLogEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/admin/audit-log"));

        assertThat(hasAuditLogEndpoints)
                .withFailMessage("Admin audit log endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document role and permission endpoints")
    void shouldDocumentRoleAndPermissionEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasRoleEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/admin/roles"));

        boolean hasPermissionEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/admin/permissions"));

        assertThat(hasRoleEndpoints)
                .withFailMessage("Role management endpoints should be documented")
                .isTrue();

        assertThat(hasPermissionEndpoints)
                .withFailMessage("Permission management endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document organization settings endpoints")
    void shouldDocumentOrganizationSettingsEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasSettingsEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/organizations") && path.contains("settings"));

        assertThat(hasSettingsEndpoints)
                .withFailMessage("Organization settings endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("Should document identity provider endpoints")
    void shouldDocumentIdentityProviderEndpoints() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        boolean hasIdentityEndpoints = paths.keySet().stream()
                .anyMatch(path -> path.startsWith("/api/v1/identity"));

        assertThat(hasIdentityEndpoints)
                .withFailMessage("Identity provider endpoints should be documented")
                .isTrue();
    }

    @Test
    @DisplayName("All documented paths should have defined operations")
    void allDocumentedPathsShouldHaveDefinedOperations() {
        Paths paths = openAPI.getPaths();
        assertThat(paths).isNotNull();

        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            long operationCount = pathItem.readOperations().size();

            assertThat(operationCount)
                    .withFailMessage("Path %s should have at least one operation defined", path)
                    .isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Should have security scheme configured for protected endpoints")
    void shouldHaveSecuritySchemeConfiguredForProtectedEndpoints() {
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    }
}
