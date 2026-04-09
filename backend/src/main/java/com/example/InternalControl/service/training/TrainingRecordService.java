package com.example.InternalControl.service.training;

import java.util.List;

import com.example.InternalControl.dto.training.TrainingRecordRequest;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;

/**
 * Service interface for managing employee training and certification records.
 * <p>
 * Tracks employee training requirements for both IK-MAT (food hygiene) and
 * IK-ALKOHOL (responsible alcohol service) compliance. Key features:
 * <ul>
 *   <li>Training record creation and assignment</li>
 *   <li>Completion tracking with certificate upload</li>
 *   <li>Expiration monitoring and alerts</li>
 *   <li>Status tracking (ASSIGNED, COMPLETED, EXPIRED)</li>
 * </ul>
 * <p>
 * Training types include: Food hygiene, alcohol service, allergen awareness,
 * first aid, fire safety, and custom organizational requirements.
 *
 * @author TriTacLe
 * @version 1.0
 * @since 1.0
 */
public interface TrainingRecordService {

    /**
     * Creates a new training record for an employee.
     * <p>
     * The record is created in ASSIGNED status with optional expiration date.
     * Notifications are sent to the assigned employee.
     *
     * @param request       the training record data (type, title, assignee, due date)
     * @param orgNumber     the organization number for scoping
     * @param currentUserId the ID of the user creating the record (for audit)
     * @return the created TrainingRecord with generated ID
     * @throws jakarta.persistence.EntityNotFoundException if user or organization not found
     * @throws IllegalArgumentException                    if required fields are missing
     */
    TrainingRecord createTrainingRecord(TrainingRecordRequest request, Integer orgNumber, Long currentUserId);

    /**
     * Retrieves a specific training record by ID.
     *
     * @param id        the ID of the training record
     * @param orgNumber the organization number for access validation
     * @return the TrainingRecord with full details
     * @throws jakarta.persistence.EntityNotFoundException if record not found
     */
    TrainingRecord getTrainingRecord(Long id, Integer orgNumber);

    /**
     * Updates an existing training record.
     * <p>
     * Can modify title, type, due date, or other metadata.
     * Cannot modify if record is already completed.
     *
     * @param id          the ID of the record to update
     * @param request     the updated training data
     * @param orgNumber   the organization number for access validation
     * @return the updated TrainingRecord
     * @throws jakarta.persistence.EntityNotFoundException if record not found
     * @throws IllegalStateException                       if record is already completed
     */
    TrainingRecord updateTrainingRecord(Long id, TrainingRecordRequest request, Integer orgNumber);

    /**
     * Deletes a training record.
     * <p>
     * Only allowed for records in ASSIGNED status.
     * Completed records are retained for compliance audit trail.
     *
     * @param id        the ID of the record to delete
     * @param orgNumber the organization number for access validation
     * @throws jakarta.persistence.EntityNotFoundException if record not found
     * @throws IllegalStateException                       if record is already completed
     */
    void deleteTrainingRecord(Long id, Integer orgNumber);

    /**
     * Retrieves all training records for an organization.
     *
     * @param orgNumber the organization number
     * @return list of all training records
     */
    List<TrainingRecord> getTrainingRecordsByOrg(Integer orgNumber);

    /**
     * Retrieves training records for a specific user across all organizations.
     * <p>
     * Use with caution - may return records from multiple organizations.
     * Prefer {@link #getTrainingRecordsByUserAndOrg} for scoped queries.
     *
     * @param userId the ID of the user
     * @return list of user's training records
     */
    List<TrainingRecord> getTrainingRecordsByUser(Long userId);

    /**
     * Retrieves training records for a user within an organization.
     * <p>
     * This is the preferred method for displaying a user's
     * training dashboard within their organization.
     *
     * @param userId    the ID of the user
     * @param orgNumber the organization number for scoping
     * @return list of user's training records in the organization
     */
    List<TrainingRecord> getTrainingRecordsByUserAndOrg(Long userId, Integer orgNumber);

    /**
     * Retrieves training records filtered by status.
     * <p>
     * Useful for dashboard widgets showing:
     * - Assigned (pending) trainings
     * - Completed trainings
     * - Expired certifications requiring renewal
     *
     * @param orgNumber the organization number
     * @param status    the status to filter by (ASSIGNED, COMPLETED, EXPIRED)
     * @return list of training records with the specified status
     */
    List<TrainingRecord> getTrainingRecordsByStatus(Integer orgNumber, TrainingStatus status);

    /**
     * Marks a training record as completed.
     * <p>
     * Records completion timestamp, sets status to COMPLETED,
     * and optionally links to a certificate document.
     * Calculates expiration date based on training type.
     *
     * @param id                   the ID of the training record
     * @param orgNumber            the organization number for access validation
     * @param certificateDocumentId optional ID of uploaded certificate (null if none)
     * @return the completed TrainingRecord
     * @throws jakarta.persistence.EntityNotFoundException if record not found
     * @throws IllegalStateException                       if already completed
     */
    TrainingRecord completeTrainingRecord(Long id, Integer orgNumber, Long certificateDocumentId);

    /**
     * Retrieves training records expiring within a threshold.
     * <p>
     * Used for proactive notifications about upcoming expirations.
     * Default threshold is typically 30 days.
     *
     * @param orgNumber      the organization number
     * @param daysThreshold  number of days to look ahead (e.g., 30 for next month)
     * @return list of training records expiring within the threshold
     */
    List<TrainingRecord> getExpiringTrainingRecords(Integer orgNumber, int daysThreshold);

    /**
     * Counts training records expiring soon.
     * <p>
     * Efficient count for dashboard badges and alerts.
     * Uses default threshold (typically 30 days).
     *
     * @param orgNumber the organization number
     * @return count of expiring training records
     */
    Long getExpiringCount(Integer orgNumber);
}
