package com.example.InternalControl.service.training;

import com.example.InternalControl.dto.training.TrainingCatalogItemResponse;
import com.example.InternalControl.repository.training.TrainingCatalogItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainingCatalogServiceImpl implements TrainingCatalogService {

    private final TrainingCatalogItemRepository trainingCatalogItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrainingCatalogItemResponse> getActiveCatalog() {
        return trainingCatalogItemRepository.findByIsActiveTrueOrderBySortOrderAscDisplayNameAsc()
                .stream()
                .map(item -> TrainingCatalogItemResponse.builder()
                        .trainingType(item.getTrainingType())
                        .displayName(item.getDisplayName())
                        .description(item.getDescription())
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }
}
