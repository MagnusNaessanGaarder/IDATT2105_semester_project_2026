package com.example.InternalControl.repository.temperature;

import com.example.InternalControl.model.temperature.TemperatureLogPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing temperature log points where measurements are taken.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface TemperatureLogPointRepository extends JpaRepository<TemperatureLogPoint, Long> {

  List<TemperatureLogPoint> findByOrgNumber(Integer orgNumber);

  List<TemperatureLogPoint> findByOrgNumberAndIsActiveTrue(Integer orgNumber);

  Optional<TemperatureLogPoint> findByLogPointIdAndOrgNumber(Long logPointId, Integer orgNumber);

}
