package com.example.InternalControl.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for organization settings.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSettingsResponse {

    private Integer orgNumber;
    private String timezoneName;
    private String localeCode;
    private Boolean enableFoodModule;
    private Boolean enableAlcoholModule;
    private BigDecimal defaultTempMinC;
    private BigDecimal defaultTempMaxC;
    private Boolean reminderEmailEnabled;
    private String notificationEmail;
    private Integer retentionUserMonths;
    private Integer retentionAuditMonths;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}