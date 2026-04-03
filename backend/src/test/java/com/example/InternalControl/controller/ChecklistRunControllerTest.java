package com.example.InternalControl.controller;

import com.example.InternalControl.dto.checklist.request.ChecklistRunCreateRequest;
import com.example.InternalControl.dto.checklist.request.ChecklistRunItemUpdateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistRunResponse;
import com.example.InternalControl.service.checklist.mapper.ChecklistRunMapper;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.shared.enums.RunStatus;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.checklist.ChecklistRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.InternalControl.controller.checklist.ChecklistRunController;

/**
 * Unit tests for ChecklistRunController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ChecklistRunControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChecklistRunService runService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private ChecklistRunMapper runMapper;

    @InjectMocks
    private ChecklistRunController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void getRuns_WithValidRequest_ReturnsList() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        ChecklistRun run = createTestRun();
        ChecklistRunResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(runService.getRunsByOrg(orgNumber)).thenReturn(List.of(run));
        when(runMapper.toResponseList(any())).thenReturn(List.of(response));

        // When & Then
        mockMvc.perform(get("/api/checklists/runs")
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].runId").value(1))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void getRuns_WithStatusFilter_ReturnsFilteredList() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        RunStatus status = RunStatus.COMPLETED;
        ChecklistRun run = createTestRun();
        ChecklistRunResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(runService.getRunsByStatus(orgNumber, status)).thenReturn(List.of(run));
        when(runMapper.toResponseList(any())).thenReturn(List.of(response));

        // When & Then
        mockMvc.perform(get("/api/checklists/runs")
                        .param("orgNumber", orgNumber.toString())
                        .param("status", status.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(1));
    }

    @Test
    void getRunById_WithValidId_ReturnsRun() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Long runId = 1L;
        ChecklistRun run = createTestRun();
        ChecklistRunResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(runService.getRun(runId, orgNumber)).thenReturn(run);
        when(runMapper.toResponse(run)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/checklists/runs/{id}", runId)
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(1));
    }

    @Test
    void createRun_WithValidRequest_ReturnsCreated() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        ChecklistRunCreateRequest request = createTestCreateRequest();
        ChecklistRun created = createTestRun();
        ChecklistRunResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(runService.createRun(any(), eq(orgNumber), eq(userId), any())).thenReturn(created);
        when(runMapper.toResponse(created)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/checklists/runs")
                        .param("orgNumber", orgNumber.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.runId").value(1));
    }

    @Test
    void completeRun_WithValidId_ReturnsCompleted() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Long runId = 1L;
        ChecklistRun run = createTestRun();
        ChecklistRunResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(runService.completeRun(runId, orgNumber)).thenReturn(run);
        when(runMapper.toResponse(run)).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/checklists/runs/{id}/complete", runId)
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(1));
    }

    @Test
    void getRunItems_WithValidId_ReturnsItems() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Long runId = 1L;
        ChecklistRun run = createTestRun();
        ChecklistRunResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(runService.getRun(runId, orgNumber)).thenReturn(run);
        when(runMapper.toResponse(run)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/checklists/runs/{id}/items", runId)
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk());
    }

    private ChecklistRun createTestRun() {
        return ChecklistRun.builder()
                .runId(1L)
                .orgNumber(123456789)
                .performedByUserId(1L)
                .runDate(LocalDate.now())
                .status(RunStatus.DRAFT)
                .items(Collections.emptyList())
                .build();
    }

    private ChecklistRunResponse createTestResponse() {
        return ChecklistRunResponse.builder()
                .runId(1L)
                .orgNumber(123456789)
                .performedByUserId(1L)
                .runDate(LocalDate.now())
                .status(RunStatus.DRAFT)
                .items(Collections.emptyList())
                .build();
    }

    private ChecklistRunCreateRequest createTestCreateRequest() {
        return ChecklistRunCreateRequest.builder()
                .templateId(1L)
                .runDate(LocalDate.now())
                .build();
    }
}
