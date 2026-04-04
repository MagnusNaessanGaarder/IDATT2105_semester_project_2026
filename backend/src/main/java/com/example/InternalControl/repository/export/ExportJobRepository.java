package com.example.InternalControl.repository.export;

import com.example.InternalControl.model.export.ExportJob;
import com.example.InternalControl.model.export.ExportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for export jobs.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

  /**
   * Find all export jobs for an organization.
   */
  Page<ExportJob> findByOrgNumber(Integer orgNumber, Pageable pageable);

  /**
   * Find export job by ID and organization.
   */
  Optional<ExportJob> findByExportJobIdAndOrgNumber(Long exportJobId, Integer orgNumber);

  /**
   * Find jobs by status created before a certain time.
   */
  List<ExportJob> findByStatusAndRequestedAtBefore(ExportStatus status, LocalDateTime before);

  /**
   * Count pending jobs for an organization.
   */
  long countByOrgNumberAndStatus(Integer orgNumber, ExportStatus status);
}
