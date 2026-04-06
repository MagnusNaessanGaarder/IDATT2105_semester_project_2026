package com.example.InternalControl.dto.settings;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrganizationSettingsResponse {

    private long orgNumber;
    private String timezoneName;
    private String localeCode;
    private boolean enableFoodModule;
    private boolean enableAlcoholModule;
    private BigDecimal defaultTempMinC;
    private BigDecimal defaultTempMaxC;
    private boolean reminderEmailEnabled;
    private String notificationEmail;
    private Long retentionUserMonths;
    private Long retentionAuditMonths;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
