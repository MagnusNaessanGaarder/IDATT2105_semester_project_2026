package com.example.InternalControl.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class BlobStorageService {

  private final BlobServiceClient blobServiceClient;

  public BlobStorageService(BlobServiceClient blobServiceClient) {
    this.blobServiceClient = blobServiceClient;
  }

  public String getContainerName(int orgNumber) {
    return "org-" + orgNumber;
  }

  private BlobContainerClient getContainerForTenant(int orgNumber) {
    String containerName = getContainerName(orgNumber);
    BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
    container.createIfNotExists();
    return container;
  }

  public String uploadFile(int orgNumber, String directory,
                           String fileName, InputStream data, long length,
                           String contentType) {
    BlobContainerClient container = getContainerForTenant(orgNumber);
    String blobName = directory + "/" + fileName;

    BlobClient blobClient = container.getBlobClient(blobName);
    blobClient.upload(data, length, true);
    blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));

    return blobName;
  }

  public ByteArrayOutputStream downloadFile(int orgNumber, String blobName) {
    BlobContainerClient container = getContainerForTenant(orgNumber);
    BlobClient blobClient = container.getBlobClient(blobName);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    blobClient.downloadStream(outputStream);
    return outputStream;
  }

  public String generateSasUrl(int orgNumber, String blobName, Duration validFor) {
    BlobContainerClient container = getContainerForTenant(orgNumber);
    BlobClient blobClient = container.getBlobClient(blobName);

    BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
    OffsetDateTime expiry = OffsetDateTime.now().plus(validFor);
    BlobServiceSasSignatureValues sasValues =
        new BlobServiceSasSignatureValues(expiry, permission);

    return blobClient.getBlobUrl() + "?" + blobClient.generateSas(sasValues);
  }

  public void deleteFile(int orgNumber, String blobName) {
    BlobContainerClient container = getContainerForTenant(orgNumber);
    container.getBlobClient(blobName).deleteIfExists();
  }
}