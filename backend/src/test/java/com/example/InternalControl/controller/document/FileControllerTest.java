package com.example.InternalControl.controller.document;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.document.OrganizationDocumentVersionRepository;
import com.example.InternalControl.service.document.DocumentService;
import com.example.InternalControl.service.storage.BlobStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FileController using TestContainers.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private BlobStorageService blobStorageService;

    @MockBean
    private OrganizationDocumentRepository documentRepo;

    @MockBean
    private OrganizationDocumentVersionRepository versionRepo;

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listDocuments_WithValidOrgNumber_ReturnsDocuments() throws Exception {
        // Given
        OrganizationDocument doc = new OrganizationDocument();
        doc.setDocumentId(1L);
        doc.setTitle("Test Document");
        doc.setOrgNumber(123456789);

        when(documentRepo.findByOrgNumberAndActiveTrue(123456789))
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
        OrganizationDocument doc = new OrganizationDocument();
        doc.setDocumentId(1L);
        doc.setTitle("Policy Document");
        doc.setOrgNumber(123456789);

        when(documentRepo.findByOrgNumberAndDocumentType(123456789, "POLICY"))
                .thenReturn(List.of(doc));

        // When & Then
        mockMvc.perform(get("/api/files")
                        .header("X-Org-Number", "123456789")
                        .param("category", "POLICY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Policy Document"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listDocuments_WithNoDocuments_ReturnsEmptyList() throws Exception {
        // Given
        when(documentRepo.findByOrgNumberAndActiveTrue(123456789))
                .thenReturn(java.util.Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/files")
                        .header("X-Org-Number", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
