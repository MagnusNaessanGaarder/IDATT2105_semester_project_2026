package com.example.InternalControl.repository.temperature;

import com.example.InternalControl.model.temperature.TemperatureLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TemperatureLogEntryRepository extends JpaRepository<TemperatureLogEntry, Long> {

  List<TemperatureLogEntry> findByOrgNumberOrderByMeasuredAtDesc(Integer orgNumber);

  List<TemperatureLogEntry> findByOrgNumberAndLogPointIdOrderByMeasuredAtDesc(Integer orgNumber, Long logPointId);

  List<TemperatureLogEntry> findByOrgNumberAndMeasuredAtBetween(Integer orgNumber, LocalDateTime from, LocalDateTime to);

  List<TemperatureLogEntry> findByOrgNumberAndIsAlertTrueOrderByMeasuredAtDesc(Integer orgNumber);

  Page<TemperatureLogEntry> findByOrgNumberOrderByMeasuredAtDesc(Integer orgNumber, Pageable pageable);

  Optional<TemperatureLogEntry> findByEntryIdAndOrgNumber(Long entryId, Integer orgNumber);

}
