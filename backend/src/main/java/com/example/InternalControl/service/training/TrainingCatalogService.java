package com.example.InternalControl.service.training;

import com.example.InternalControl.dto.training.TrainingCatalogItemResponse;
import java.util.List;

public interface TrainingCatalogService {
    List<TrainingCatalogItemResponse> getActiveCatalog();
}
