package com.example.InternalControl.controller.training;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.training.TrainingRecordRequest;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;
import com.example.InternalControl.model.training.TrainingType;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.training.TrainingRecordService;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TrainingRecordController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TrainingRecordControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainingRecordService trainingRecordService;

    @MockBean
    private UserOrganizationService userOrgService;

    private static final Integer ORG_NUMBER = 937219997;
    private static final String BASE_URL = "/api/v1/training";

    @BeforeEach
    void setUp() {
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null) {
            CustomUserDetails userDetails = new CustomUserDetails(
                    1L, existingAuth.getName(), "password", existingAuth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        }
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getTrainingRecords_AsEmployee_ReturnsOk() throws Exception {
        // Given
        when(trainingRecordService.getTrainingRecordsByOrg(ORG_NUMBER)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getTrainingRecord_AsEmployee_ReturnsOk() throws Exception {
        // Given
        TrainingRecord record = new TrainingRecord();
        record.setTrainingRecordId(1L);
        record.setTitle("Training");

        when(trainingRecordService.getTrainingRecord(1L, ORG_NUMBER)).thenReturn(record);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingRecordId").value(1));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void createTrainingRecord_AsManager_ReturnsCreated() throws Exception {
        // Given
        TrainingRecordRequest request = new TrainingRecordRequest();
        request.setUserId(1L);
        request.setTrainingType(TrainingType.FOOD_HYGIENE);
        request.setTitle("Food Hygiene Training");

        TrainingRecord record = new TrainingRecord();
        record.setTrainingRecordId(1L);
        record.setTitle("Food Hygiene Training");

        when(trainingRecordService.createTrainingRecord(any(), eq(ORG_NUMBER), anyLong()))
                .thenReturn(record);

        // When & Then
        mockMvc.perform(post(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trainingRecordId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void createTrainingRecord_AsEmployee_ReturnsForbidden() throws Exception {
        // Given
        TrainingRecordRequest request = new TrainingRecordRequest();
        request.setUserId(1L);
        request.setTrainingType(TrainingType.FOOD_HYGIENE);
        request.setTitle("Food Hygiene Training");

        // When & Then
        mockMvc.perform(post(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void updateTrainingRecord_AsManager_ReturnsOk() throws Exception {
        // Given
        TrainingRecordRequest request = new TrainingRecordRequest();
        request.setUserId(1L);
        request.setTrainingType(TrainingType.FOOD_HYGIENE);
        request.setTitle("Updated Title");

        TrainingRecord record = new TrainingRecord();
        record.setTrainingRecordId(1L);
        record.setTitle("Updated Title");

        when(trainingRecordService.updateTrainingRecord(eq(1L), any(), eq(ORG_NUMBER)))
                .thenReturn(record);

        // When & Then
        mockMvc.perform(put(BASE_URL + "/1")
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteTrainingRecord_AsAdmin_ReturnsNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete(BASE_URL + "/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void completeTrainingRecord_AsManager_ReturnsOk() throws Exception {
        // Given
        TrainingRecord record = new TrainingRecord();
        record.setTrainingRecordId(1L);
        record.setStatus(TrainingStatus.COMPLETED);

        when(trainingRecordService.completeTrainingRecord(1L, ORG_NUMBER, null))
                .thenReturn(record);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/1/complete")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void completeTrainingRecord_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then - only ADMIN/MANAGER can complete
        mockMvc.perform(post(BASE_URL + "/1/complete")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isForbidden());
    }
}
