package com.example.InternalControl.dto.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrganizationSettingsRequest {

    @NotBlank(message = "Timezone is required")
    private String timezoneName;

    @NotBlank(message = "Locale is required")
    private String localeCode;

    @NotNull(message = "Food module enablement is required")
    private Boolean enableFoodModule;

    @NotNull(message = "Alcohol module enablement is required")
    private Boolean enableAlcoholModule;

    private BigDecimal defaultTempMinC;

    private BigDecimal defaultTempMaxC;

    @NotNull(message = "Email reminder setting is required")
    private Boolean reminderEmailEnabled;

    @Email(message = "Invalid email format")
    private String notificationEmail;

    private Integer retentionUserMonths;

    private Integer retentionAuditMonths;
}
