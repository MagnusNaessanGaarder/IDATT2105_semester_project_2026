package com.example.InternalControl.controller.export;

import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.model.export.ExportType;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.export.ExportService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = ExportController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExportService exportService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserOrganizationService userOrgService;

    private ExportResponse mockResponse;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Mock userOrgService to return true for organization access
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
        
        mockResponse = ExportResponse.builder()
                .exportJobId(1L)
                .exportType(ExportType.CHECKLIST_REPORT)
                .format(ExportFormat.PDF)
                .status(ExportStatus.PENDING)
                .build();
    }

    @Test

    void createExport_ValidRequest_ReturnsCreated() throws Exception {
        when(exportService.createExportJob(any(), anyInt(), anyLong())).thenReturn(mockResponse);

        ExportRequest request = new ExportRequest();
        request.setExportType(ExportType.CHECKLIST_REPORT);
        request.setFormat(ExportFormat.PDF);

        mockMvc.perform(post("/api/v1/exports")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exportJobId").value(1));
    }

    @Test

    void getExportStatus_ExistingJob_ReturnsOk() throws Exception {
        when(exportService.getExportStatus(anyLong(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/exports/1/status")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test

    void getDownloadUrl_CompletedJob_ReturnsOk() throws Exception {
        when(exportService.getDownloadUrl(anyLong(), anyInt())).thenReturn("https://example.com/download");

        mockMvc.perform(get("/api/v1/exports/1/download")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void createExport_Unauthenticated_ReturnsUnauthorized() throws Exception {
        ExportRequest request = new ExportRequest();
        request.setExportType(ExportType.CHECKLIST_REPORT);
        request.setFormat(ExportFormat.PDF);

        mockMvc.perform(post("/api/v1/exports")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test

    void createExport_AsEmployee_ReturnsForbidden() throws Exception {
        ExportRequest request = new ExportRequest();
        request.setExportType(ExportType.CHECKLIST_REPORT);
        request.setFormat(ExportFormat.PDF);

        mockMvc.perform(post("/api/v1/exports")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
