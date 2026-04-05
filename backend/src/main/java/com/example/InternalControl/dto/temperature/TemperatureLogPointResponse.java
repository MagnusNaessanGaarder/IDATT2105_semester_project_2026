package com.example.InternalControl.dto.temperature;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TemperatureLogPointResponse {

  private Long logPointId;
  private Long locationId;
  private String locationName;
  private String name;
  private Boolean isActive;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
