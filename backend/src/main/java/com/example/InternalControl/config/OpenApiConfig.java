package com.example.InternalControl.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for API documentation.
 * Configures API metadata and JWT security scheme.
 * Access documentation at http://localhost:8080/swagger-ui.html when running.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
        .info(new Info()
            .title("IK-Kontroll API")
            .version("1.0")
            .description("API for restaurant internkontroll system. " +
                "Authentication with JWT. See README.md for test user information.")
            .contact(new Contact()
                .name("IDATT2105 Frivillig prosjekt 2026")
                .email("trile@stud.ntnu.no")))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(new Components()
            .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")));
  }
}
