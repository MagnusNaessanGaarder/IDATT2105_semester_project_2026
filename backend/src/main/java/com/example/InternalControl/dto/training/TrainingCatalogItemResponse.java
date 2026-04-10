package com.example.InternalControl.dto.training;

import com.example.InternalControl.model.training.TrainingType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrainingCatalogItemResponse {
    TrainingType trainingType;
    String displayName;
    String description;
    Integer sortOrder;
}
