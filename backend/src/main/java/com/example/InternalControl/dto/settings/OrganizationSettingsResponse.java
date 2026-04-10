package com.example.InternalControl.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSettingsResponse {

    private int orgNumber;
    private String timezoneName;
    private String localeCode;
    private boolean enableFoodModule;
    private boolean enableAlcoholModule;
    private BigDecimal defaultTempMinC;
    private BigDecimal defaultTempMaxC;
    private boolean reminderEmailEnabled;
    private String notificationEmail;
    private String displayName;
    private String legalName;
    private String contactEmail;
    private String contactPhone;
    private Integer retentionUserMonths;
    private Integer retentionAuditMonths;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
