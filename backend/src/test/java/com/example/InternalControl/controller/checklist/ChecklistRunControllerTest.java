package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistRunCreateRequest;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.checklist.ChecklistRunService;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ChecklistRunController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(ChecklistRunController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChecklistRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChecklistRunService runService;

    @MockBean
    private UserOrganizationService userOrgService;

    @BeforeEach
    void setUp() {
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getRuns_WithValidRequest_ReturnsRuns() throws Exception {
        // Given
        ChecklistRun run = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.DRAFT)
                .orgNumber(123456789)
                .build();

        when(runService.getRunsByOrg(123456789))
                .thenReturn(List.of(run));

        // When & Then
        mockMvc.perform(get("/api/checklists/runs")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getRuns_WithStatusFilter_ReturnsFilteredRuns() throws Exception {
        // Given
        ChecklistRun run = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.COMPLETED)
                .orgNumber(123456789)
                .build();

        when(runService.getRunsByStatus(123456789, RunStatus.COMPLETED))
                .thenReturn(List.of(run));

        // When & Then
        mockMvc.perform(get("/api/checklists/runs")
                        .param("orgNumber", "123456789")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getRun_WithValidId_ReturnsRun() throws Exception {
        // Given
        ChecklistRun run = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.DRAFT)
                .build();

        when(runService.getRun(1L, 123456789))
                .thenReturn(run);

        // When & Then
        mockMvc.perform(get("/api/checklists/runs/1")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(1));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void createRun_WithValidRequest_ReturnsCreatedRun() throws Exception {
        // Given
        ChecklistRunCreateRequest request = ChecklistRunCreateRequest.builder()
                .templateId(1L)
                .runDate(LocalDate.now())
                .build();

        ChecklistRun created = ChecklistRun.builder()
                .runId(1L)
                .templateId(1L)
                .status(RunStatus.DRAFT)
                .build();

        when(runService.createRun(anyLong(), anyInt(), anyLong(), any()))
                .thenReturn(created);

        // When & Then
        mockMvc.perform(post("/api/checklists/runs")
                        .param("orgNumber", "123456789")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void completeRun_WithValidId_ReturnsCompletedRun() throws Exception {
        // Given
        ChecklistRun completed = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.COMPLETED)
                .build();

        when(runService.completeRun(1L, 123456789))
                .thenReturn(completed);

        // When & Then
        mockMvc.perform(put("/api/checklists/runs/1/complete")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getRunItems_WithValidId_ReturnsItems() throws Exception {
        // Given
        ChecklistRun run = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.DRAFT)
                .build();

        when(runService.getRun(1L, 123456789))
                .thenReturn(run);

        // When & Then
        mockMvc.perform(get("/api/checklists/runs/1/items")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk());
    }

    @Test
    void getRuns_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/checklists/runs")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isUnauthorized());
    }
}