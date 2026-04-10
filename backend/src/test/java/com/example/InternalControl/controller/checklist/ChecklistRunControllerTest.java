package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistRunCreateRequest;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.repository.checklist.ChecklistTemplateItemRepository;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.checklist.ChecklistRunService;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
@WebMvcTest(controllers = ChecklistRunController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
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

    @MockBean
    private ChecklistTemplateItemRepository templateItemRepository;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    private void authenticateAs(String role) {
        CustomUserDetails userDetails = new CustomUserDetails(
                1L,
                "test@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
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
        mockMvc.perform(get("/api/v1/checklists/runs")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(1));
    }

    @Test
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
        mockMvc.perform(get("/api/v1/checklists/runs")
                        .param("orgNumber", "123456789")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getRun_WithValidId_ReturnsRun() throws Exception {
        // Given
        ChecklistRun run = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.DRAFT)
                .orgNumber(123456789)
                .build();

        when(runService.getRun(1L, 123456789))
                .thenReturn(run);

        // When & Then
        mockMvc.perform(get("/api/v1/checklists/runs/1")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(1));
    }

    @Test
    void createRun_WithValidRequest_ReturnsCreatedRun() throws Exception {
        // Given
        authenticateAs("ROLE_MANAGER");
        ChecklistRunCreateRequest request = ChecklistRunCreateRequest.builder()
                .templateId(1L)
                .runDate(LocalDate.now())
                .build();

        ChecklistRun created = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.DRAFT)
                .orgNumber(123456789)
                .build();

        when(runService.createRun(anyLong(), anyInt(), anyLong(), any()))
                .thenReturn(created);

        // When & Then
        mockMvc.perform(post("/api/v1/checklists/runs")
                        .param("orgNumber", "123456789")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(1));
    }

    @Test
    void completeRun_WithValidId_ReturnsCompletedRun() throws Exception {
        // Given
        ChecklistRun completed = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.COMPLETED)
                .orgNumber(123456789)
                .build();

        when(runService.completeRun(1L, 123456789, 1L))
                .thenReturn(completed);

        // When & Then
        mockMvc.perform(put("/api/v1/checklists/runs/1/complete")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getRunItems_WithValidId_ReturnsItems() throws Exception {
        // Given
        ChecklistRun run = ChecklistRun.builder()
                .runId(1L)
                .status(RunStatus.DRAFT)
                .orgNumber(123456789)
                .build();

        when(runService.getRun(1L, 123456789))
                .thenReturn(run);

        // When & Then
        mockMvc.perform(get("/api/v1/checklists/runs/1/items")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk());
    }

    @Test
    void getRuns_WithInvalidOrg_ReturnsNotFound() throws Exception {
        // Given
        when(userOrgService.isUserInOrganization(anyLong(), anyInt()))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/checklists/runs")
                        .param("orgNumber", "999999999"))
                .andExpect(status().isNotFound());
    }
}
