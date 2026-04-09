package com.example.InternalControl.repository.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.InternalControl.model.organization.Location;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing physical locations within an organization.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

  List<Location> findByOrgNumber(Integer orgNumber);

  List<Location> findByOrgNumberAndIsActiveTrue(Integer orgNumber);

  Optional<Location> findByLocationIdAndOrgNumber(Long locationId, Integer orgNumber);

}
