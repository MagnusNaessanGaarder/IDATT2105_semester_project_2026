package com.example.InternalControl.controller.document;

import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.document.OrganizationDocumentVersionRepository;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.document.DocumentService;
import com.example.InternalControl.service.storage.BlobStorageService;
import com.example.InternalControl.service.user.UserOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
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
@WebMvcTest(controllers = FileController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

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

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserOrganizationService userOrgService;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    void listDocuments_WithValidOrgNumber_ReturnsDocuments() throws Exception {
        // Given
        OrganizationDocument doc = new OrganizationDocument();
        doc.setDocumentId(1L);
        doc.setTitle("Test Document");
        doc.setOrgNumber(123456789);

        when(documentRepo.findByOrgNumberAndActiveTrue(123456789))
                .thenReturn(List.of(doc));

        // When & Then
        mockMvc.perform(get("/api/v1/files")
                        .header("X-Org-Number", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(1));
    }

    @Test
    void listDocuments_WithCategoryFilter_ReturnsFilteredDocuments() throws Exception {
        // Given
        OrganizationDocument doc = new OrganizationDocument();
        doc.setDocumentId(1L);
        doc.setTitle("Policy Document");
        doc.setOrgNumber(123456789);

        when(documentRepo.findByOrgNumberAndDocumentType(123456789, "POLICY"))
                .thenReturn(List.of(doc));

        // When & Then
        mockMvc.perform(get("/api/v1/files")
                        .param("category", "POLICY")
                        .header("X-Org-Number", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Policy Document"));
    }

    @Test
    void listDocuments_WithNoDocuments_ReturnsEmptyList() throws Exception {
        // Given
        when(documentRepo.findByOrgNumberAndActiveTrue(123456789))
                .thenReturn(java.util.Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/files")
                        .header("X-Org-Number", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
