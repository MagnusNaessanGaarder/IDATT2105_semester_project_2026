package com.example.InternalControl.dto.document;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for document download information.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class DocumentDownloadDto {
  private Long documentId;
  private String originalFilename;
  private String mimeType;
  private String azureBlobName;
  private Long fileSizeBytes;
}
