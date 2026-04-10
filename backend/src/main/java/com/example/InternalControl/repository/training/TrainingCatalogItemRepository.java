package com.example.InternalControl.repository.training;

import com.example.InternalControl.model.training.TrainingCatalogItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingCatalogItemRepository extends JpaRepository<TrainingCatalogItem, Long> {
    List<TrainingCatalogItem> findByIsActiveTrueOrderBySortOrderAscDisplayNameAsc();
}
