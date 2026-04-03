package com.example.InternalControl.repository.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.InternalControl.model.location.Location;

import java.util.List;
import java.util.Optional;

/**
 * Repository for locations.
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
