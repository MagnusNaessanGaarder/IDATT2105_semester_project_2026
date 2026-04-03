package com.example.InternalControl.service.document;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Service for Azure Blob Storage operations.
 * Only available when AZURE_STORAGE_CONNECTION_STRING is configured.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@ConditionalOnBean(BlobServiceClient.class)
@Slf4j
public class BlobStorageService {

  private final BlobServiceClient blobServiceClient;

  public BlobStorageService(BlobServiceClient blobServiceClient) {
    this.blobServiceClient = blobServiceClient;
    log.info("BlobStorageService initialized with Azure Blob Storage");
  }

  public String getContainerName(int orgNumber) {
    return "org-" + orgNumber;
  }

  private BlobContainerClient getOrCreateContainer(int orgNumber) {
    String containerName = getContainerName(orgNumber);
    BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
    container.createIfNotExists();
    return container;
  }

  private BlobContainerClient getExistingContainer(int orgNumber) {
    String containerName = getContainerName(orgNumber);
    BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
    if (!container.exists()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "No storage found for organization " + orgNumber);
    }
    return container;
  }

  private BlobClient getExistingBlob(int orgNumber, String blobName) {
    BlobContainerClient container = getExistingContainer(orgNumber);
    BlobClient blobClient = container.getBlobClient(blobName);
    if (!blobClient.exists()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "File not found: " + blobName);
    }
    return blobClient;
  }

  public String uploadFile(int orgNumber, String directory,
                           String fileName, InputStream data, long length,
                           String contentType) {
    BlobContainerClient container = getOrCreateContainer(orgNumber);
    String blobName = directory + "/" + fileName;

    BlobClient blobClient = container.getBlobClient(blobName);
    blobClient.upload(data, length, true);
    blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));

    return blobName;
  }

  public ByteArrayOutputStream downloadFile(int orgNumber, String blobName) {
    BlobClient blobClient = getExistingBlob(orgNumber, blobName);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    blobClient.downloadStream(outputStream);
    return outputStream;
  }

  public String generateSasUrl(int orgNumber, String blobName, Duration validFor) {
    BlobClient blobClient = getExistingBlob(orgNumber, blobName);

    BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
    OffsetDateTime expiry = OffsetDateTime.now().plus(validFor);
    BlobServiceSasSignatureValues sasValues =
        new BlobServiceSasSignatureValues(expiry, permission);

    return blobClient.getBlobUrl() + "?" + blobClient.generateSas(sasValues);
  }

  public void deleteFile(int orgNumber, String blobName) {
    BlobContainerClient container = getExistingContainer(orgNumber);
    container.getBlobClient(blobName).deleteIfExists();
  }
}