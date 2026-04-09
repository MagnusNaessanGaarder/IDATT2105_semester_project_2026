package com.example.InternalControl.dto.temperature.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for temperature log entry requests.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureLogEntryRequest {

  @NotNull(message = "Log point ID is required")
  private Long logPointId;

  @NotNull(message = "Temperature is required")
  private BigDecimal temperatureC;

  private LocalDateTime measuredAt;

  private String noteText;

  private Long recordedByUserId;
}
