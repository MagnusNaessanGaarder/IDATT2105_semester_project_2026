package com.example.InternalControl.service.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Handles document storage and retrieval in Azure Blob Storage.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
public class BlobStorageService {

  private final BlobServiceClient blobServiceClient;

  public BlobStorageService(@Nullable BlobServiceClient blobServiceClient) {
    this.blobServiceClient = blobServiceClient;
  }

  private void checkConfigured() {
    if (blobServiceClient == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Azure Storage is not configured. Please configure AZURE_STORAGE_CONNECTION_STRING.");
    }
  }

  public String getContainerName(int orgNumber) {
    return "org-" + orgNumber;
  }

  /**
   * Gets or creates the container - use only for upload.
   */
  private BlobContainerClient getOrCreateContainer(int orgNumber) {
    checkConfigured();
    String containerName = getContainerName(orgNumber);
    BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
    container.createIfNotExists();
    return container;
  }

  /**
   * Gets an existing container - use for read/delete operations.
   * Throws 404 if the container doesn't exist.
   */
  private BlobContainerClient getExistingContainer(int orgNumber) {
    checkConfigured();
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
