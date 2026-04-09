package com.example.InternalControl.dto.temperature.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for temperature log entry responses.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureLogEntryResponse {

  private Long entryId;
  private Long logPointId;
  private String logPointName;
  private Long locationId;
  private String locationName;
  private BigDecimal temperatureC;
  private Boolean isAlert;
  private String noteText;
  private String recordedByName;
  private LocalDateTime measuredAt;
  private LocalDateTime createdAt;
}
