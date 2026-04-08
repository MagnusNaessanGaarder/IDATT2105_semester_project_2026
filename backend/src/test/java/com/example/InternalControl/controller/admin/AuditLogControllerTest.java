package com.example.InternalControl.controller.admin;

import com.example.InternalControl.controller.admin.AuditLogController;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.AuditLog;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.audit.AuditLogService;
import com.example.InternalControl.service.user.UserOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = AuditLogController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private UserOrganizationService userOrgService;

    @MockBean
    private JwtService jwtService;

    private AuditLog mockAuditLog;

    @BeforeEach
    void setUp() {
        mockAuditLog = AuditLog.builder()
                .auditLogId(1L)
                .actionType("CREATE")
                .entityType("DEVIATION_REPORT")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAuditLogsByOrganization_AsAdmin_ReturnsOk() throws Exception {
        List<AuditLog> logs = Arrays.asList(mockAuditLog);
        when(auditLogService.getAuditLogsByOrganization(anyInt())).thenReturn(logs);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value(1));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getAuditLogsByOrganization_AsManager_ReturnsOk() throws Exception {
        List<AuditLog> logs = Arrays.asList(mockAuditLog);
        when(auditLogService.getAuditLogsByOrganization(anyInt())).thenReturn(logs);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getAuditLogsByOrganization_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAuditLogsByActionType_ReturnsFilteredLogs() throws Exception {
        List<AuditLog> logs = Arrays.asList(mockAuditLog);
        when(auditLogService.getAuditLogsByActionType(anyInt(), any())).thenReturn(logs);

        mockMvc.perform(get("/api/v1/audit-logs/by-action")
                        .param("orgNumber", "937219997")
                        .param("actionType", "CREATE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAuditLogsByUser_ReturnsUserLogs() throws Exception {
        List<AuditLog> logs = Arrays.asList(mockAuditLog);
        when(auditLogService.getAuditLogsByUser(anyLong())).thenReturn(logs);

        mockMvc.perform(get("/api/v1/audit-logs/by-user/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getAuditLogsByOrganization_AsEmployee_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isForbidden());
    }
}
