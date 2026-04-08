package com.example.InternalControl.service.temperature;

import com.example.InternalControl.dto.temperature.request.TemperatureLogPointRequest;
import com.example.InternalControl.dto.temperature.request.TemperatureLogEntryRequest;
import com.example.InternalControl.dto.temperature.response.TemperatureLogPointResponse;
import com.example.InternalControl.dto.temperature.response.TemperatureLogEntryResponse;
import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.model.temperature.TemperatureLogEntry;
import com.example.InternalControl.model.temperature.TemperatureLogPoint;
import com.example.InternalControl.repository.organization.LocationRepository;
import com.example.InternalControl.repository.temperature.TemperatureLogEntryRepository;
import com.example.InternalControl.repository.temperature.TemperatureLogPointRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TemperatureLogServiceImpl implements TemperatureLogService {

  private final TemperatureLogPointRepository pointRepository;
  private final TemperatureLogEntryRepository entryRepository;
  private final LocationRepository locationRepository;

  @Override
  public TemperatureLogPointResponse createLogPoint(TemperatureLogPointRequest request, Integer orgNumber) {
    Location location = locationRepository.findById(request.getLocationId())
        .orElseThrow(() -> new EntityNotFoundException("Location not found: " + request.getLocationId()));

    if (!location.getOrgNumber().equals(orgNumber)) {
      throw new EntityNotFoundException("Location not found in organization");
    }

    TemperatureLogPoint point = TemperatureLogPoint.builder()
        .orgNumber(orgNumber)
        .locationId(request.getLocationId())
        .name(request.getName())
        .isActive(request.getIsActive() != null ? request.getIsActive() : true)
        .build();

    TemperatureLogPoint saved = pointRepository.save(point);
    return mapToPointResponse(saved, location);
  }

  @Override
  public TemperatureLogPointResponse updateLogPoint(Long pointId, TemperatureLogPointRequest request, Integer orgNumber) {
    TemperatureLogPoint point = pointRepository.findByLogPointIdAndOrgNumber(pointId, orgNumber)
        .orElseThrow(() -> new EntityNotFoundException("Temperature log point not found: " + pointId));

    point.setName(request.getName());
    if (request.getIsActive() != null) {
      point.setIsActive(request.getIsActive());
    }

    TemperatureLogPoint saved = pointRepository.save(point);
    Location location = locationRepository.findById(saved.getLocationId()).orElse(null);
    return mapToPointResponse(saved, location);
  }

  @Override
  public void deleteLogPoint(Long pointId, Integer orgNumber) {
    TemperatureLogPoint point = pointRepository.findByLogPointIdAndOrgNumber(pointId, orgNumber)
        .orElseThrow(() -> new EntityNotFoundException("Temperature log point not found: " + pointId));
    pointRepository.delete(point);
  }

  @Override
  @Transactional(readOnly = true)
  public TemperatureLogPointResponse getLogPoint(Long pointId, Integer orgNumber) {
    TemperatureLogPoint point = pointRepository.findByLogPointIdAndOrgNumber(pointId, orgNumber)
        .orElseThrow(() -> new EntityNotFoundException("Temperature log point not found: " + pointId));
    Location location = locationRepository.findById(point.getLocationId()).orElse(null);
    return mapToPointResponse(point, location);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemperatureLogPointResponse> listLogPoints(Integer orgNumber) {
    return pointRepository.findByOrgNumber(orgNumber).stream()
        .map(point -> {
          Location location = locationRepository.findById(point.getLocationId()).orElse(null);
          return mapToPointResponse(point, location);
        })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemperatureLogPointResponse> listActiveLogPoints(Integer orgNumber) {
    return pointRepository.findByOrgNumberAndIsActiveTrue(orgNumber).stream()
        .map(point -> {
          Location location = locationRepository.findById(point.getLocationId()).orElse(null);
          return mapToPointResponse(point, location);
        })
        .collect(Collectors.toList());
  }

  @Override
  public TemperatureLogEntryResponse recordEntry(TemperatureLogEntryRequest request, Integer orgNumber, Long userId) {
    TemperatureLogPoint point = pointRepository.findByLogPointIdAndOrgNumber(request.getLogPointId(), orgNumber)
        .orElseThrow(() -> new EntityNotFoundException("Temperature log point not found: " + request.getLogPointId()));

    Location location = locationRepository.findById(point.getLocationId()).orElse(null);
    boolean isAlert = checkIfAlert(request.getTemperatureC(), location);

    TemperatureLogEntry entry = TemperatureLogEntry.builder()
        .orgNumber(orgNumber)
        .logPointId(request.getLogPointId())
        .recordedByUserId(userId)
        .measuredAt(request.getMeasuredAt() != null ? request.getMeasuredAt() : LocalDateTime.now())
        .temperatureC(request.getTemperatureC())
        .isAlert(isAlert)
        .noteText(request.getNoteText())
        .build();

    TemperatureLogEntry saved = entryRepository.save(entry);

    if (isAlert) {
      log.warn("Temperature alert recorded for point {}: {}C (outside range {} - {})",
          point.getLogPointId(), request.getTemperatureC(),
          location != null ? location.getTempMinC() : "N/A",
          location != null ? location.getTempMaxC() : "N/A");
    }

    return mapToEntryResponse(saved, point, location);
  }

  private boolean checkIfAlert(BigDecimal temperature, Location location) {
    if (location == null || temperature == null) {
      return false;
    }
    BigDecimal minTemp = location.getTempMinC();
    BigDecimal maxTemp = location.getTempMaxC();

    if (minTemp == null && maxTemp == null) {
      return false;
    }

    boolean belowMin = minTemp != null && temperature.compareTo(minTemp) < 0;
    boolean aboveMax = maxTemp != null && temperature.compareTo(maxTemp) > 0;

    return belowMin || aboveMax;
  }

  @Override
  @Transactional(readOnly = true)
  public TemperatureLogEntryResponse getEntry(Long entryId, Integer orgNumber) {
    TemperatureLogEntry entry = entryRepository.findByEntryIdAndOrgNumber(entryId, orgNumber)
        .orElseThrow(() -> new EntityNotFoundException("Temperature log entry not found: " + entryId));
    return mapToEntryResponseWithPoint(entry, orgNumber);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemperatureLogEntryResponse> listEntries(Integer orgNumber) {
    return entryRepository.findByOrgNumberOrderByMeasuredAtDesc(orgNumber).stream()
        .map(entry -> mapToEntryResponseWithPoint(entry, orgNumber))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemperatureLogEntryResponse> listEntriesByPoint(Long pointId, Integer orgNumber) {
    return entryRepository.findByOrgNumberAndLogPointIdOrderByMeasuredAtDesc(orgNumber, pointId).stream()
        .map(entry -> mapToEntryResponseWithPoint(entry, orgNumber))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemperatureLogEntryResponse> listEntriesByDateRange(Integer orgNumber, LocalDateTime from, LocalDateTime to) {
    return entryRepository.findByOrgNumberAndMeasuredAtBetween(orgNumber, from, to).stream()
        .map(entry -> mapToEntryResponseWithPoint(entry, orgNumber))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemperatureLogEntryResponse> listAlerts(Integer orgNumber) {
    return entryRepository.findByOrgNumberAndIsAlertTrueOrderByMeasuredAtDesc(orgNumber).stream()
        .map(entry -> mapToEntryResponseWithPoint(entry, orgNumber))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TemperatureLogEntryResponse> listEntriesPaginated(Integer orgNumber, Pageable pageable) {
    return entryRepository.findByOrgNumberOrderByMeasuredAtDesc(orgNumber, pageable)
        .map(entry -> mapToEntryResponseWithPoint(entry, orgNumber));
  }

  private TemperatureLogPointResponse mapToPointResponse(TemperatureLogPoint point, Location location) {
    return TemperatureLogPointResponse.builder()
        .logPointId(point.getLogPointId())
        .locationId(point.getLocationId())
        .locationName(location != null ? location.getName() : null)
        .name(point.getName())
        .isActive(point.getIsActive())
        .createdAt(point.getCreatedAt())
        .updatedAt(point.getUpdatedAt())
        .build();
  }

  private TemperatureLogEntryResponse mapToEntryResponse(TemperatureLogEntry entry, TemperatureLogPoint point, Location location) {
    return TemperatureLogEntryResponse.builder()
        .entryId(entry.getEntryId())
        .logPointId(entry.getLogPointId())
        .logPointName(point != null ? point.getName() : null)
        .locationId(point != null ? point.getLocationId() : null)
        .locationName(location != null ? location.getName() : null)
        .temperatureC(entry.getTemperatureC())
        .isAlert(entry.getIsAlert())
        .noteText(entry.getNoteText())
        .measuredAt(entry.getMeasuredAt())
        .createdAt(entry.getCreatedAt())
        .build();
  }

  private TemperatureLogEntryResponse mapToEntryResponseWithPoint(TemperatureLogEntry entry, Integer orgNumber) {
    TemperatureLogPoint point = pointRepository.findByLogPointIdAndOrgNumber(entry.getLogPointId(), orgNumber).orElse(null);
    Location location = point != null ? locationRepository.findById(point.getLocationId()).orElse(null) : null;
    return mapToEntryResponse(entry, point, location);
  }
}
