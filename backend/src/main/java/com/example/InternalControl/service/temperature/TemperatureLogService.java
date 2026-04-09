package com.example.InternalControl.service.temperature;

import com.example.InternalControl.dto.temperature.request.TemperatureLogPointRequest;
import com.example.InternalControl.dto.temperature.request.TemperatureLogEntryRequest;
import com.example.InternalControl.dto.temperature.response.TemperatureLogPointResponse;
import com.example.InternalControl.dto.temperature.response.TemperatureLogEntryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for temperature logging and monitoring.
 * <p>
 * Temperature logging is critical for IK-MAT compliance, ensuring food
 * storage temperatures remain within safe ranges. The service manages:
 * <ul>
 *   <li>Temperature log points (locations to monitor)</li>
 *   <li>Temperature entries (recorded measurements)</li>
 *   <li>Automatic alert generation for out-of-range temperatures</li>
 *   <li>Historical data retrieval and pagination</li>
 * </ul>
 * <p>
 * Typical usage: Refrigerators, freezers, and hot-holding equipment
 * are configured as log points. Staff record temperatures at scheduled
 * intervals, and the system alerts when temperatures exceed thresholds.
 *
 * @author TriTacLe
 * @version 1.0
 * @since 1.0
 */
public interface TemperatureLogService {

    // ==================== LOG POINTS ====================

    /**
     * Creates a new temperature log point (monitoring location).
     * <p>
     * Log points define locations where temperatures are monitored,
     * along with acceptable min/max temperature ranges.
     *
     * @param request   the log point data including name, location, and temperature limits
     * @param orgNumber the organization number for scoping
     * @return the created TemperatureLogPointResponse with generated ID
     * @throws jakarta.persistence.EntityNotFoundException if organization or location not found
     * @throws IllegalArgumentException                    if temperature range is invalid
     */
    TemperatureLogPointResponse createLogPoint(TemperatureLogPointRequest request, Integer orgNumber);

    /**
     * Updates an existing temperature log point.
     * <p>
     * Can modify name, location, or temperature thresholds.
     * Changes affect future temperature readings only.
     *
     * @param pointId   the ID of the log point to update
     * @param request   the updated log point data
     * @param orgNumber the organization number for access validation
     * @return the updated TemperatureLogPointResponse
     * @throws jakarta.persistence.EntityNotFoundException if log point not found
     */
    TemperatureLogPointResponse updateLogPoint(Long pointId, TemperatureLogPointRequest request, Integer orgNumber);

    /**
     * Deactivates (soft-deletes) a temperature log point.
     * <p>
     * Historical entries are preserved. The point is marked inactive
     * and won't appear in active monitoring lists.
     *
     * @param pointId   the ID of the log point to deactivate
     * @param orgNumber the organization number for access validation
     * @throws jakarta.persistence.EntityNotFoundException if log point not found
     */
    void deleteLogPoint(Long pointId, Integer orgNumber);

    /**
     * Retrieves a specific temperature log point by ID.
     *
     * @param pointId   the ID of the log point
     * @param orgNumber the organization number for access validation
     * @return the TemperatureLogPointResponse with current configuration
     * @throws jakarta.persistence.EntityNotFoundException if log point not found
     */
    TemperatureLogPointResponse getLogPoint(Long pointId, Integer orgNumber);

    /**
     * Retrieves all temperature log points for an organization.
     *
     * @param orgNumber the organization number
     * @return list of all log points (both active and inactive)
     */
    List<TemperatureLogPointResponse> listLogPoints(Integer orgNumber);

    /**
     * Retrieves only active temperature log points.
     * <p>
     * Use this for daily monitoring tasks to show only
     * currently monitored locations.
     *
     * @param orgNumber the organization number
     * @return list of active log points
     */
    List<TemperatureLogPointResponse> listActiveLogPoints(Integer orgNumber);

    // ==================== ENTRIES ====================

    /**
     * Records a temperature reading.
     * <p>
     * Automatically checks if the temperature is within the acceptable
     * range for the log point. Creates an alert if out of range.
     * Records the user who made the entry for audit trail.
     *
     * @param request   the temperature entry data (point ID, temperature, timestamp)
     * @param orgNumber the organization number for access validation
     * @param userId    the ID of the user recording the temperature
     * @return the created TemperatureLogEntryResponse with alert status
     * @throws jakarta.persistence.EntityNotFoundException if log point not found
     * @throws IllegalArgumentException                    if temperature value is invalid
     */
    TemperatureLogEntryResponse recordEntry(TemperatureLogEntryRequest request, Integer orgNumber, Long userId);

    /**
     * Retrieves a specific temperature entry by ID.
     *
     * @param entryId   the ID of the temperature entry
     * @param orgNumber the organization number for access validation
     * @return the TemperatureLogEntryResponse
     * @throws jakarta.persistence.EntityNotFoundException if entry not found
     */
    TemperatureLogEntryResponse getEntry(Long entryId, Integer orgNumber);

    /**
     * Retrieves all temperature entries for an organization.
     * <p>
     * Returns entries sorted by measurement time (newest first).
     *
     * @param orgNumber the organization number
     * @return list of temperature entries
     */
    List<TemperatureLogEntryResponse> listEntries(Integer orgNumber);

    /**
     * Retrieves entries for a specific log point.
     * <p>
     * Useful for viewing temperature history of a specific
     * refrigerator or storage location.
     *
     * @param pointId   the ID of the log point
     * @param orgNumber the organization number for access validation
     * @return list of entries for the specified point
     * @throws jakarta.persistence.EntityNotFoundException if log point not found
     */
    List<TemperatureLogEntryResponse> listEntriesByPoint(Long pointId, Integer orgNumber);

    /**
     * Retrieves entries within a date range.
     * <p>
     * Useful for generating reports for inspections or analysis.
     *
     * @param orgNumber the organization number
     * @param from      start of date range (inclusive)
     * @param to        end of date range (inclusive)
     * @return list of entries within the date range
     */
    List<TemperatureLogEntryResponse> listEntriesByDateRange(Integer orgNumber, LocalDateTime from, LocalDateTime to);

    /**
     * Retrieves entries that triggered temperature alerts.
     * <p>
     * Returns only entries where temperature was outside
     * the acceptable range for the log point.
     *
     * @param orgNumber the organization number
     * @return list of entries with alerts, sorted by alert time (newest first)
     */
    List<TemperatureLogEntryResponse> listAlerts(Integer orgNumber);

    /**
     * Retrieves paginated temperature entries.
     * <p>
     * Use for large datasets where loading all entries at once
     * would be inefficient. Supports sorting and filtering.
     *
     * @param orgNumber the organization number
     * @param pageable  pagination parameters (page number, size, sort)
     * @return page of temperature entries
     */
    Page<TemperatureLogEntryResponse> listEntriesPaginated(Integer orgNumber, Pageable pageable);
}
