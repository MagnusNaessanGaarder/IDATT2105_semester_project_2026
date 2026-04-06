package com.example.InternalControl.controller.document;

import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.service.document.DocumentService;
import com.example.InternalControl.service.storage.BlobStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FileController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private BlobStorageService blobStorageService;

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listDocuments_WithValidOrgNumber_ReturnsDocuments() throws Exception {
        // Given
        OrganizationDocument doc = OrganizationDocument.builder()
                .documentId(1L)
                .title("Test Document")
                .orgNumber(123456789)
                .build();

        when(documentService.findByOrgNumberAndActiveTrue(123456789))
                .thenReturn(List.of(doc));

        // When & Then
        mockMvc.perform(get("/api/files")
                        .header("X-Org-Number", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listDocuments_WithCategoryFilter_ReturnsFilteredDocuments() throws Exception {
        // Given
        OrganizationDocument doc = OrganizationDocument.builder()
                .documentId(1L)
                .title("Policy Document")
                .orgNumber(123456789)
                .build();

        when(documentService.findByOrgNumberAndDocumentType(123456789, "POLICY"))
                .thenReturn(List.of(doc));

        // When & Then
        mockMvc.perform(get("/api/files")
                        .header("X-Org-Number", "123456789")
                        .param("category", "POLICY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Policy Document"));
    }

    @Test
    void listDocuments_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/files")
                        .header("X-Org-Number", "123456789"))
                .andExpect(status().isUnauthorized());
    }
}