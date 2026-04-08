package com.example.InternalControl.service.temperature;

import com.example.InternalControl.dto.temperature.TemperatureLogEntryRequest;
import com.example.InternalControl.dto.temperature.TemperatureLogEntryResponse;
import com.example.InternalControl.dto.temperature.TemperatureLogPointRequest;
import com.example.InternalControl.dto.temperature.TemperatureLogPointResponse;
import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.model.temperature.TemperatureLogEntry;
import com.example.InternalControl.model.temperature.TemperatureLogPoint;
import com.example.InternalControl.repository.organization.LocationRepository;
import com.example.InternalControl.repository.temperature.TemperatureLogEntryRepository;
import com.example.InternalControl.repository.temperature.TemperatureLogPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemperatureLogServiceTest {

  @Mock
  private TemperatureLogPointRepository pointRepository;

  @Mock
  private TemperatureLogEntryRepository entryRepository;

  @Mock
  private LocationRepository locationRepository;

  @InjectMocks
  private TemperatureLogServiceImpl temperatureLogService;

  private static final Integer ORG_NUMBER = 123456789;
  private static final Long USER_ID = 1L;

  @BeforeEach
  void setUp() {
  }

  @Test
  void createLogPoint_WithValidRequest_ReturnsResponse() {
    TemperatureLogPointRequest request = TemperatureLogPointRequest.builder()
        .locationId(1L)
        .name("Fridge 1")
        .build();

    Location location = Location.builder()
        .locationId(1L)
        .orgNumber(ORG_NUMBER)
        .name("Kitchen")
        .build();

    TemperatureLogPoint savedPoint = TemperatureLogPoint.builder()
        .logPointId(1L)
        .orgNumber(ORG_NUMBER)
        .locationId(1L)
        .name("Fridge 1")
        .isActive(true)
        .build();

    when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
    when(pointRepository.save(any())).thenReturn(savedPoint);

    TemperatureLogPointResponse response = temperatureLogService.createLogPoint(request, ORG_NUMBER);

    assertNotNull(response);
    assertEquals("Fridge 1", response.getName());
    assertEquals(1L, response.getLogPointId());
  }

  @Test
  void recordEntry_WithNormalTemperature_NoAlert() {
    TemperatureLogEntryRequest request = TemperatureLogEntryRequest.builder()
        .logPointId(1L)
        .temperatureC(new BigDecimal("4.5"))
        .measuredAt(LocalDateTime.now())
        .build();

    Location location = Location.builder()
        .locationId(1L)
        .orgNumber(ORG_NUMBER)
        .tempMinC(new BigDecimal("2.0"))
        .tempMaxC(new BigDecimal("8.0"))
        .build();

    TemperatureLogPoint point = TemperatureLogPoint.builder()
        .logPointId(1L)
        .orgNumber(ORG_NUMBER)
        .locationId(1L)
        .location(location)
        .build();

    TemperatureLogEntry savedEntry = TemperatureLogEntry.builder()
        .entryId(1L)
        .orgNumber(ORG_NUMBER)
        .logPointId(1L)
        .temperatureC(new BigDecimal("4.5"))
        .isAlert(false)
        .build();

    when(pointRepository.findByLogPointIdAndOrgNumber(1L, ORG_NUMBER)).thenReturn(Optional.of(point));
    when(entryRepository.save(any())).thenReturn(savedEntry);

    TemperatureLogEntryResponse response = temperatureLogService.recordEntry(request, ORG_NUMBER, USER_ID);

    assertNotNull(response);
    assertFalse(response.getIsAlert());
    assertEquals(new BigDecimal("4.5"), response.getTemperatureC());
  }

  @Test
  void recordEntry_WithOutOfRangeTemperature_CreatesAlert() {
    TemperatureLogEntryRequest request = TemperatureLogEntryRequest.builder()
        .logPointId(1L)
        .temperatureC(new BigDecimal("15.0"))
        .measuredAt(LocalDateTime.now())
        .build();

    Location location = Location.builder()
        .locationId(1L)
        .orgNumber(ORG_NUMBER)
        .tempMinC(new BigDecimal("2.0"))
        .tempMaxC(new BigDecimal("8.0"))
        .build();

    TemperatureLogPoint point = TemperatureLogPoint.builder()
        .logPointId(1L)
        .orgNumber(ORG_NUMBER)
        .locationId(1L)
        .location(location)
        .build();

    TemperatureLogEntry savedEntry = TemperatureLogEntry.builder()
        .entryId(1L)
        .orgNumber(ORG_NUMBER)
        .logPointId(1L)
        .temperatureC(new BigDecimal("15.0"))
        .isAlert(true)
        .build();

    when(pointRepository.findByLogPointIdAndOrgNumber(1L, ORG_NUMBER)).thenReturn(Optional.of(point));
    when(entryRepository.save(any())).thenReturn(savedEntry);

    TemperatureLogEntryResponse response = temperatureLogService.recordEntry(request, ORG_NUMBER, USER_ID);

    assertNotNull(response);
    assertTrue(response.getIsAlert());
  }

  @Test
  void recordEntry_WithoutLocationTemperatureRange_NoAlertCheck() {
    TemperatureLogEntryRequest request = TemperatureLogEntryRequest.builder()
        .logPointId(1L)
        .temperatureC(new BigDecimal("25.0"))
        .measuredAt(LocalDateTime.now())
        .build();

    Location location = Location.builder()
        .locationId(1L)
        .orgNumber(ORG_NUMBER)
        .tempMinC(null)
        .tempMaxC(null)
        .build();

    TemperatureLogPoint point = TemperatureLogPoint.builder()
        .logPointId(1L)
        .orgNumber(ORG_NUMBER)
        .locationId(1L)
        .location(location)
        .build();

    TemperatureLogEntry savedEntry = TemperatureLogEntry.builder()
        .entryId(1L)
        .orgNumber(ORG_NUMBER)
        .logPointId(1L)
        .temperatureC(new BigDecimal("25.0"))
        .isAlert(false)
        .build();

    when(pointRepository.findByLogPointIdAndOrgNumber(1L, ORG_NUMBER)).thenReturn(Optional.of(point));
    when(entryRepository.save(any())).thenReturn(savedEntry);

    TemperatureLogEntryResponse response = temperatureLogService.recordEntry(request, ORG_NUMBER, USER_ID);

    assertNotNull(response);
    assertFalse(response.getIsAlert());
  }

  @Test
  void listAlerts_ReturnsOnlyAlertEntries() {
    TemperatureLogEntry alertEntry = TemperatureLogEntry.builder()
        .entryId(1L)
        .orgNumber(ORG_NUMBER)
        .logPointId(1L)
        .temperatureC(new BigDecimal("15.0"))
        .isAlert(true)
        .measuredAt(LocalDateTime.now())
        .build();

    when(entryRepository.findByOrgNumberAndIsAlertTrueOrderByMeasuredAtDesc(ORG_NUMBER))
        .thenReturn(Collections.singletonList(alertEntry));

    var alerts = temperatureLogService.listAlerts(ORG_NUMBER);

    assertNotNull(alerts);
    assertEquals(1, alerts.size());
    assertTrue(alerts.get(0).getIsAlert());
  }
}
