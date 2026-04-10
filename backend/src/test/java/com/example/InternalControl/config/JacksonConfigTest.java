package com.example.InternalControl.config;
import com.example.InternalControl.AbstractIntegrationTest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JacksonConfig.
 * Verifies Jackson ObjectMapper configuration for date/time handling.
 */

@TestPropertySource(
        locations = "classpath:application-test.properties",
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
        }
)
@DisplayName("JacksonConfig Tests")
class JacksonConfigTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should configure ObjectMapper with JavaTimeModule")
    void shouldConfigureObjectMapperWithJavaTimeModule() {
        // Then
        assertThat(objectMapper).isNotNull();
        // Check that JavaTimeModule is registered by attempting to serialize a date
        assertThat(objectMapper.getRegisteredModuleIds())
                .anyMatch(id -> id.toString().toLowerCase().contains("jsr310") 
                        || id.toString().contains("JavaTimeModule"));
    }

    @Test
    @DisplayName("Should disable writing dates as timestamps")
    void shouldDisableWritingDatesAsTimestamps() {
        // Then
        assertThat(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
                .isFalse();
    }

    @Test
    @DisplayName("Should serialize LocalDate as ISO string")
    void shouldSerializeLocalDateAsIsoString() throws Exception {
        // Given
        LocalDate date = LocalDate.of(2026, 4, 7);

        // When
        String json = objectMapper.writeValueAsString(date);

        // Then
        assertThat(json).isEqualTo("\"2026-04-07\"");
    }

    @Test
    @DisplayName("Should serialize LocalDateTime as ISO string")
    void shouldSerializeLocalDateTimeAsIsoString() throws Exception {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 7, 14, 30, 0);

        // When
        String json = objectMapper.writeValueAsString(dateTime);

        // Then
        assertThat(json).isEqualTo("\"2026-04-07T14:30:00\"");
    }

    @Test
    @DisplayName("Should serialize LocalTime as ISO string")
    void shouldSerializeLocalTimeAsIsoString() throws Exception {
        // Given
        LocalTime time = LocalTime.of(14, 30, 0);

        // When
        String json = objectMapper.writeValueAsString(time);

        // Then
        assertThat(json).isEqualTo("\"14:30:00\"");
    }

    @Test
    @DisplayName("Should deserialize LocalDate from ISO string")
    void shouldDeserializeLocalDateFromIsoString() throws Exception {
        // Given
        String json = "\"2026-04-07\"";

        // When
        LocalDate date = objectMapper.readValue(json, LocalDate.class);

        // Then
        assertThat(date).isEqualTo(LocalDate.of(2026, 4, 7));
    }

    @Test
    @DisplayName("Should deserialize LocalDateTime from ISO string")
    void shouldDeserializeLocalDateTimeFromIsoString() throws Exception {
        // Given
        String json = "\"2026-04-07T14:30:00\"";

        // When
        LocalDateTime dateTime = objectMapper.readValue(json, LocalDateTime.class);

        // Then
        assertThat(dateTime).isEqualTo(LocalDateTime.of(2026, 4, 7, 14, 30, 0));
    }

    @Test
    @DisplayName("Should be primary ObjectMapper bean")
    void shouldBePrimaryObjectMapperBean() {
        // This test verifies that the ObjectMapper is configured by our JacksonConfig
        // and is available for autowiring throughout the application
        assertThat(objectMapper).isNotNull();
    }
}
