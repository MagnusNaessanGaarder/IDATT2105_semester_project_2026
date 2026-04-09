package com.example.InternalControl.dto.temperature;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response containing temperature measurement details with location context.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
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
