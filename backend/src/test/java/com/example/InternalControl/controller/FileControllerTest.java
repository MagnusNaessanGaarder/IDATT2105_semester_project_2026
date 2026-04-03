package com.example.InternalControl.controller;

import com.example.InternalControl.dto.document.DocumentDownloadDto;
import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.document.BlobStorageService;
import com.example.InternalControl.service.document.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.InternalControl.controller.document.FileController;

/**
 * Unit tests for FileController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DocumentService documentService;

    @Mock
    private BlobStorageService blobStorageService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private FileController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void upload_WithValidFile_ReturnsDocument() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );
        OrganizationDocument doc = createTestDocument();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(documentService.uploadDocument(eq(orgNumber), any(), eq("policy"), eq("documents")))
                .thenReturn(doc);

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("orgNumber", orgNumber.toString())
                        .param("documentType", "policy")
                        .param("directory", "documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(1))
                .andExpect(jsonPath("$.originalFilename").value("test-document.pdf"));
    }

    @Test
    void download_WithValidId_ReturnsFile() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Long documentId = 1L;
        DocumentDownloadDto downloadDto = createTestDownloadDto();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("PDF content".getBytes());

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(documentService.getDocumentForDownload(documentId, orgNumber)).thenReturn(downloadDto);
        when(blobStorageService.downloadFile(orgNumber, "test-blob-name")).thenReturn(stream);

        // When & Then
        mockMvc.perform(get("/api/files/download/{documentId}", documentId)
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test-document.pdf\""));
    }

    @Test
    void upload_WithoutFile_ReturnsBadRequest() throws Exception {
        // Given
        Integer orgNumber = 123456789;

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isBadRequest());
    }

    private OrganizationDocument createTestDocument() {
        OrganizationDocument doc = new OrganizationDocument();
        doc.setDocumentId(1L);
        doc.setOrgNumber(123456789);
        doc.setOriginalFilename("test-document.pdf");
        doc.setMimeType(MediaType.APPLICATION_PDF_VALUE);
        doc.setDocumentType("policy");
        return doc;
    }

    private DocumentDownloadDto createTestDownloadDto() {
        return DocumentDownloadDto.builder()
                .documentId(1L)
                .originalFilename("test-document.pdf")
                .azureBlobName("test-blob-name")
                .mimeType(MediaType.APPLICATION_PDF_VALUE)
                .build();
    }
}
