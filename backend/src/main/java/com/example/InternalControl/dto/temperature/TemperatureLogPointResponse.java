package com.example.InternalControl.dto.temperature;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response containing temperature log point information with location details.
 *
 * @author TriTacLe
 * @since 1.0
 */
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
