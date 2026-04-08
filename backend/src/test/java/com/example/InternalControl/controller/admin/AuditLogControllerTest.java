package com.example.InternalControl.controller.admin;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.audit.AuditLog;
import com.example.InternalControl.service.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuditLogController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    private static final Integer ORG_NUMBER = 123456789;
    private static final String BASE_URL = "/api/v1/admin/audit-logs";

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAuditLogs_AsAdmin_ReturnsOk() throws Exception {
        // Given
        AuditLog log = new AuditLog();
        log.setAuditLogId(1L);
        log.setActionType("CREATE");
        log.setCreatedAt(LocalDateTime.now());

        when(auditLogService.getAuditLogsByOrganization(ORG_NUMBER)).thenReturn(List.of(log));

        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].auditLogId").value(1));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getAuditLogs_AsManager_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAuditLogsByUser_AsAdmin_ReturnsOk() throws Exception {
        // Given
        when(auditLogService.getAuditLogsByUser(1L)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get(BASE_URL + "/user/1"))
                .andExpect(status().isOk());
    }
}
