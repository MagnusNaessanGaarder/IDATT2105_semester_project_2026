package com.example.InternalControl.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OpenApiConfig.
 * Verifies Swagger/OpenAPI configuration is correctly set up.
 */
@SpringBootTest
@TestPropertySource(
        locations = "classpath:application-test.properties",
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@DisplayName("OpenApiConfig Tests")
class OpenApiConfigTest {

    @Autowired
    private OpenAPI openAPI;

    @Test
    @DisplayName("Should configure OpenAPI with correct title")
    void shouldConfigureOpenApiWithCorrectTitle() {
        // When
        Info info = openAPI.getInfo();

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getTitle()).isEqualTo("IK-Kontroll API");
    }

    @Test
    @DisplayName("Should configure OpenAPI with correct version")
    void shouldConfigureOpenApiWithCorrectVersion() {
        // When
        Info info = openAPI.getInfo();

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getVersion()).isEqualTo("1.0");
    }

    @Test
    @DisplayName("Should configure OpenAPI with description")
    void shouldConfigureOpenApiWithDescription() {
        // When
        Info info = openAPI.getInfo();

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getDescription()).contains("API for restaurant internkontroll system");
        assertThat(info.getDescription()).contains("JWT");
    }

    @Test
    @DisplayName("Should configure OpenAPI with contact information")
    void shouldConfigureOpenApiWithContactInformation() {
        // When
        Contact contact = openAPI.getInfo().getContact();

        // Then
        assertThat(contact).isNotNull();
        assertThat(contact.getName()).isEqualTo("IDATT2105 Frivillig prosjekt 2026");
        assertThat(contact.getEmail()).isEqualTo("trile@stud.ntnu.no");
    }

    @Test
    @DisplayName("Should configure JWT security scheme")
    void shouldConfigureJwtSecurityScheme() {
        // When
        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");

        // Then
        assertThat(securityScheme).isNotNull();
        assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(securityScheme.getScheme()).isEqualTo("bearer");
        assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("Should configure security requirement")
    void shouldConfigureSecurityRequirement() {
        // Then
        assertThat(openAPI.getSecurity()).isNotEmpty();
        assertThat(openAPI.getSecurity().get(0)).containsKey("bearerAuth");
    }
}
