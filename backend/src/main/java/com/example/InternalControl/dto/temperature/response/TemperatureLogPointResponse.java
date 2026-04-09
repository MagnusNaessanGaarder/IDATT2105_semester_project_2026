package com.example.InternalControl.dto.temperature.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for temperature log point responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureLogPointResponse {

  private Long logPointId;
  private Long locationId;
  private String locationName;
  private String name;
  private Boolean isActive;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
