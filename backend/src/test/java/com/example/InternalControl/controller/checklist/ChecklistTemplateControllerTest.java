package com.example.InternalControl.controller.checklist;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.checklist.ChecklistTemplateService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ChecklistTemplateController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = ChecklistTemplateController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ChecklistTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChecklistTemplateService templateService;

    @MockBean
    private com.example.InternalControl.service.user.UserOrganizationService userOrgService;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getTemplates_WithValidRequest_ReturnsTemplates() throws Exception {
        // Given
        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateId(1L)
                .title("Daily Checklist")
                .moduleType(ModuleType.FOOD)
                .build();

        when(templateService.getTemplatesByOrg(123456789))
                .thenReturn(List.of(template));

        // When & Then
        mockMvc.perform(get("/api/checklists/templates")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateId").value(1))
                .andExpect(jsonPath("$[0].title").value("Daily Checklist"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getTemplate_WithValidId_ReturnsTemplate() throws Exception {
        // Given
        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateId(1L)
                .title("Daily Checklist")
                .moduleType(ModuleType.FOOD)
                .build();

        when(templateService.getTemplate(1L, 123456789))
                .thenReturn(template);

        // When & Then
        mockMvc.perform(get("/api/checklists/templates/1")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getTemplatesByModule_WithValidModule_ReturnsTemplates() throws Exception {
        // Given
        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateId(1L)
                .title("Food Checklist")
                .moduleType(ModuleType.FOOD)
                .build();

        when(templateService.getTemplatesByModule(123456789, ModuleType.FOOD))
                .thenReturn(List.of(template));

        // When & Then
        mockMvc.perform(get("/api/checklists/templates/module/FOOD")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moduleType").value("FOOD"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getActiveTemplates_ReturnsActiveTemplates() throws Exception {
        // Given
        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateId(1L)
                .title("Active Checklist")
                .isActive(true)
                .build();

        when(templateService.getActiveTemplates(123456789))
                .thenReturn(List.of(template));

        // When & Then
        mockMvc.perform(get("/api/checklists/templates/active")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void createTemplate_WithValidRequest_ReturnsCreatedTemplate() throws Exception {
        // Given
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title("New Template")
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .build();

        ChecklistTemplate created = ChecklistTemplate.builder()
                .templateId(1L)
                .title("New Template")
                .moduleType(ModuleType.FOOD)
                .build();

        when(templateService.createTemplate(any(), anyInt(), anyLong()))
                .thenReturn(created);

        // When & Then
        mockMvc.perform(post("/api/checklists/templates")
                        .param("orgNumber", "123456789")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void createTemplate_AsEmployee_ReturnsForbidden() throws Exception {
        // Given
        ChecklistTemplateCreateRequest request = ChecklistTemplateCreateRequest.builder()
                .title("New Template")
                .build();

        // When & Then
        mockMvc.perform(post("/api/checklists/templates")
                        .param("orgNumber", "123456789")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getTemplates_WithInvalidOrg_ReturnsNotFound() throws Exception {
        // Given
        when(userOrgService.isUserInOrganization(anyLong(), anyInt()))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/checklists/templates")
                        .param("orgNumber", "999999999"))
                .andExpect(status().isNotFound());
    }
}