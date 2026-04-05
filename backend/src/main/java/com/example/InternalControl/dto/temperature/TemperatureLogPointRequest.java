package com.example.InternalControl.dto.temperature;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureLogPointRequest {

  @NotNull(message = "Location ID is required")
  private Long locationId;

  @NotBlank(message = "Name is required")
  private String name;

  private Boolean isActive;
}
