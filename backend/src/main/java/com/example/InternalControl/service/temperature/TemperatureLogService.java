package com.example.InternalControl.service.temperature;

import com.example.InternalControl.dto.temperature.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TemperatureLogService {

  // Log Points
  TemperatureLogPointResponse createLogPoint(TemperatureLogPointRequest request, Integer orgNumber);

  TemperatureLogPointResponse updateLogPoint(Long pointId, TemperatureLogPointRequest request, Integer orgNumber);

  void deleteLogPoint(Long pointId, Integer orgNumber);

  void clearEntriesForPoint(Long pointId, Integer orgNumber);

  TemperatureLogPointResponse getLogPoint(Long pointId, Integer orgNumber);

  List<TemperatureLogPointResponse> listLogPoints(Integer orgNumber);

  List<TemperatureLogPointResponse> listActiveLogPoints(Integer orgNumber);

  // Entries
  TemperatureLogEntryResponse recordEntry(TemperatureLogEntryRequest request, Integer orgNumber, Long userId);

  TemperatureLogEntryResponse updateEntry(Long entryId, TemperatureLogEntryRequest request, Integer orgNumber, Long userId);

  TemperatureLogEntryResponse getEntry(Long entryId, Integer orgNumber);

  List<TemperatureLogEntryResponse> listEntries(Integer orgNumber);

  List<TemperatureLogEntryResponse> listEntriesByPoint(Long pointId, Integer orgNumber);

  List<TemperatureLogEntryResponse> listEntriesByDateRange(Integer orgNumber, LocalDateTime from, LocalDateTime to);

  List<TemperatureLogEntryResponse> listAlerts(Integer orgNumber);

  Page<TemperatureLogEntryResponse> listEntriesPaginated(Integer orgNumber, Pageable pageable);
}
