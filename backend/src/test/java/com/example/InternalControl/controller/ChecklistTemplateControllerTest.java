package com.example.InternalControl.controller;

import com.example.InternalControl.dto.checklist.request.ChecklistTemplateCreateRequest;
import com.example.InternalControl.dto.checklist.response.ChecklistTemplateResponse;
import com.example.InternalControl.service.checklist.mapper.ChecklistTemplateMapper;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.shared.enums.Frequency;
import com.example.InternalControl.shared.enums.ModuleType;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.checklist.ChecklistTemplateService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.InternalControl.controller.checklist.ChecklistTemplateController;

/**
 * Unit tests for ChecklistTemplateController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ChecklistTemplateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChecklistTemplateService templateService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private ChecklistTemplateMapper templateMapper;

    @InjectMocks
    private ChecklistTemplateController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getTemplates_WithValidRequest_ReturnsList() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        ChecklistTemplate template = createTestTemplate();
        ChecklistTemplateResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(templateService.getTemplatesByOrg(orgNumber)).thenReturn(List.of(template));
        when(templateMapper.toResponseList(any())).thenReturn(List.of(response));

        // When & Then
        mockMvc.perform(get("/api/checklists/templates")
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].templateId").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Template"));
    }

    @Test
    void getTemplateById_WithValidId_ReturnsTemplate() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Long templateId = 1L;
        ChecklistTemplate template = createTestTemplate();
        ChecklistTemplateResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(templateService.getTemplate(templateId, orgNumber)).thenReturn(template);
        when(templateMapper.toResponse(template)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/checklists/templates/{id}", templateId)
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.templateId").value(1))
                .andExpect(jsonPath("$.title").value("Test Template"));
    }

    @Test
    void getActiveTemplates_ReturnsActiveList() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        ChecklistTemplate template = createTestTemplate();
        ChecklistTemplateResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(templateService.getActiveTemplates(orgNumber)).thenReturn(List.of(template));
        when(templateMapper.toResponseList(any())).thenReturn(List.of(response));

        // When & Then
        mockMvc.perform(get("/api/checklists/templates/active")
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void createTemplate_WithValidRequest_ReturnsCreated() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        ChecklistTemplateCreateRequest request = createTestCreateRequest();
        ChecklistTemplate created = createTestTemplate();
        ChecklistTemplateResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(templateService.createTemplate(any(), eq(orgNumber), eq(userId))).thenReturn(created);
        when(templateMapper.toResponse(created)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/checklists/templates")
                        .param("orgNumber", orgNumber.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.templateId").value(1));
    }

    @Test
    void createTemplate_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        Integer orgNumber = 123456789;
        ChecklistTemplateCreateRequest invalidRequest = ChecklistTemplateCreateRequest.builder()
                .title("") // Invalid: empty title
                .description("Test description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();

        // When & Then
        mockMvc.perform(post("/api/checklists/templates")
                        .param("orgNumber", orgNumber.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTemplate_WithValidRequest_ReturnsUpdated() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Long templateId = 1L;
        ChecklistTemplateCreateRequest request = createTestCreateRequest();
        ChecklistTemplate updated = createTestTemplate();
        ChecklistTemplateResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(templateService.updateTemplate(eq(templateId), any(), eq(orgNumber))).thenReturn(updated);
        when(templateMapper.toResponse(updated)).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/checklists/templates/{id}", templateId)
                        .param("orgNumber", orgNumber.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(1));
    }

    @Test
    void deleteTemplate_WithValidId_ReturnsNoContent() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Long templateId = 1L;

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);

        // When & Then
        mockMvc.perform(delete("/api/checklists/templates/{id}", templateId)
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isNoContent());
    }

    private ChecklistTemplate createTestTemplate() {
        return ChecklistTemplate.builder()
                .templateId(1L)
                .orgNumber(123456789)
                .title("Test Template")
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .isActive(true)
                .items(Collections.emptyList())
                .build();
    }

    private ChecklistTemplateResponse createTestResponse() {
        return ChecklistTemplateResponse.builder()
                .templateId(1L)
                .orgNumber(123456789)
                .title("Test Template")
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .isActive(true)
                .items(Collections.emptyList())
                .build();
    }

    private ChecklistTemplateCreateRequest createTestCreateRequest() {
        return ChecklistTemplateCreateRequest.builder()
                .title("Test Template")
                .description("Test Description")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .build();
    }
}
