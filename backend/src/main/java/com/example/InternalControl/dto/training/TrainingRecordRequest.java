package com.example.InternalControl.dto.training;

import com.example.InternalControl.model.training.TrainingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrainingRecordRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Training type is required")
    private TrainingType trainingType;

    @NotBlank(message = "Title is required")
    private String title;

    private LocalDateTime completedAt;

    private LocalDateTime expiresAt;

    private Long certificateDocumentId;

    private String notes;
}
