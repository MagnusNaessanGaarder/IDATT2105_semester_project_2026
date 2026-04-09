package com.example.InternalControl.controller.training;

import com.example.InternalControl.dto.training.TrainingRecordRequest;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.training.TrainingRecordService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for training record operations.
 * Manages employee training records, certifications, and compliance tracking.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/training")
@Tag(name = "Training Records", description = "Employee training and certification management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TrainingRecordController {

    private final TrainingRecordService trainingRecordService;
    private final UserOrganizationService userOrgService;

    @Operation(summary = "Get all training records for organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved training records"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ResponseEntity<List<TrainingRecord>> getTrainingRecords(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(trainingRecordService.getTrainingRecordsByOrg(orgNumber));
    }

    @Operation(summary = "Get training record by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved training record"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Training record not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TrainingRecord> getTrainingRecord(
            @Parameter(description = "Identifier of the id")
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(trainingRecordService.getTrainingRecord(id, orgNumber));
    }

    @Operation(summary = "Get user's training records")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's training records"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TrainingRecord>> getUserTrainingRecords(
            @Parameter(description = "Identifier of the userId")
            @PathVariable Long userId,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long currentUserId = userDetails.getUserId();
        validateUserOrganizationAccess(currentUserId, orgNumber);
        return ResponseEntity.ok(trainingRecordService.getTrainingRecordsByUserAndOrg(userId, orgNumber));
    }

    @Operation(summary = "Get training records by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved training records"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TrainingRecord>> getTrainingRecordsByStatus(
            @Parameter(description = "Identifier of the status")
            @PathVariable TrainingStatus status,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(trainingRecordService.getTrainingRecordsByStatus(orgNumber, status));
    }

    @Operation(summary = "Create new training record")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Training record created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TrainingRecord> createTrainingRecord(
            @Valid @RequestBody TrainingRecordRequest request,
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        TrainingRecord created = trainingRecordService.createTrainingRecord(request, orgNumber, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getTrainingRecordId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Update training record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Training record updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Training record not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TrainingRecord> updateTrainingRecord(
            @Parameter(description = "Identifier of the id")
            @PathVariable Long id,
            @Valid @RequestBody TrainingRecordRequest request,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(trainingRecordService.updateTrainingRecord(id, request, orgNumber));
    }

    @Operation(summary = "Delete training record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Training record deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Training record not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTrainingRecord(
            @Parameter(description = "Identifier of the id")
            @PathVariable Long id,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        trainingRecordService.deleteTrainingRecord(id, orgNumber);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Complete training record (Admin/Manager only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Training record marked as completed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Manager only"),
        @ApiResponse(responseCode = "404", description = "Training record not found")
    })
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TrainingRecord> completeTrainingRecord(
            @Parameter(description = "Identifier of the id")
            @PathVariable Long id,
            @RequestParam(required = false) Long certificateDocumentId,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(trainingRecordService.completeTrainingRecord(id, orgNumber, certificateDocumentId));
    }

    @Operation(summary = "Get expiring training records")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved expiring records"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/expiring")
    public ResponseEntity<List<TrainingRecord>> getExpiringTrainingRecords(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(trainingRecordService.getExpiringTrainingRecords(orgNumber, days));
    }

    @Operation(summary = "Get count of expiring training records")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved count"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/expiring/count")
    public ResponseEntity<Map<String, Long>> getExpiringCount(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        Long count = trainingRecordService.getExpiringCount(orgNumber);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
