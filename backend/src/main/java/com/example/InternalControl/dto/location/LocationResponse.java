package com.example.InternalControl.dto.location;

import com.example.InternalControl.shared.enums.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for location.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {

    private Long locationId;

    private Integer orgNumber;

    private String name;

    private String description;

    private LocationType locationType;

    private BigDecimal tempMinC;

    private BigDecimal tempMaxC;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
