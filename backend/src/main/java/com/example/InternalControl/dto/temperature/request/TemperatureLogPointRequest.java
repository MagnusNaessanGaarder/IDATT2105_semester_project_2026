package com.example.InternalControl.dto.temperature.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO for temperature log point requests.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureLogPointRequest {

  @NotNull(message = "Location ID is required")
  private Long locationId;

  @NotBlank(message = "Name is required")
  private String name;

  private Boolean isActive;
}
