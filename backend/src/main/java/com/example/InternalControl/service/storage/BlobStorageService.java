package com.example.InternalControl.service.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@Slf4j
public class BlobStorageService {

  private final BlobServiceClient blobServiceClient;
  private final Path localStorageRoot;

  public BlobStorageService(
      @Nullable BlobServiceClient blobServiceClient,
      @Value("${storage.local.root:${java.io.tmpdir}/ik-local-storage}") String localStorageRootPath) {
    this.blobServiceClient = blobServiceClient;
    this.localStorageRoot = Paths.get(localStorageRootPath);
  }

  public String getContainerName(int orgNumber) {
    return "org-" + orgNumber;
  }

  // ── Azure helpers ────────────────────────────────────────────────────────────

  private boolean azureAvailable() {
    return blobServiceClient != null;
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

  // ── Local filesystem helpers ─────────────────────────────────────────────────

  private Path localPath(int orgNumber, String blobName) {
    return localStorageRoot.resolve(getContainerName(orgNumber)).resolve(blobName);
  }

  private String uploadLocalFile(int orgNumber, String directory, String fileName,
                                  InputStream data) {
    String blobName = directory + "/" + fileName;
    Path target = localPath(orgNumber, blobName);
    try {
      Files.createDirectories(target.getParent());
      Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
      log.info("Saved file locally: {}", target);
      return blobName;
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to save file locally: " + e.getMessage());
    }
  }

  private ByteArrayOutputStream downloadLocalFile(int orgNumber, String blobName) {
    Path source = localPath(orgNumber, blobName);
    if (!Files.exists(source)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "File not found locally: " + blobName);
    }
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Files.copy(source, out);
      return out;
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to read local file: " + e.getMessage());
    }
  }

  private void deleteLocalFile(int orgNumber, String blobName) {
    try {
      Files.deleteIfExists(localPath(orgNumber, blobName));
    } catch (IOException e) {
      log.warn("Could not delete local file {}: {}", blobName, e.getMessage());
    }
  }

  // ── Public API ───────────────────────────────────────────────────────────────

  public String uploadFile(int orgNumber, String directory,
                           String fileName, InputStream data, long length,
                           String contentType) {
    if (!azureAvailable()) {
      log.warn("Azure not configured — saving to local filesystem");
      return uploadLocalFile(orgNumber, directory, fileName, data);
    }

    // Buffer the stream so we can retry locally if Azure fails
    byte[] bytes;
    try {
      bytes = data.readAllBytes();
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read upload stream");
    }

    try {
      BlobContainerClient container = getOrCreateContainer(orgNumber);
      String blobName = directory + "/" + fileName;
      BlobClient blobClient = container.getBlobClient(blobName);
      blobClient.upload(new ByteArrayInputStream(bytes), bytes.length, true);
      blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
      return blobName;
    } catch (Exception e) {
      log.warn("Azure upload failed ({}), falling back to local filesystem", e.getMessage());
      return uploadLocalFile(orgNumber, directory, fileName, new ByteArrayInputStream(bytes));
    }
  }

  public ByteArrayOutputStream downloadFile(int orgNumber, String blobName) {
    if (!azureAvailable()) {
      return downloadLocalFile(orgNumber, blobName);
    }
    try {
      BlobClient blobClient = getExistingBlob(orgNumber, blobName);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      blobClient.downloadStream(outputStream);
      return outputStream;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Azure download failed ({}), trying local filesystem", e.getMessage());
      return downloadLocalFile(orgNumber, blobName);
    }
  }

  public String generateSasUrl(int orgNumber, String blobName, Duration validFor) {
    if (!azureAvailable()) {
      // Return a local download URL when Azure is not configured
      return "/api/v1/files/download-local?orgNumber=" + orgNumber + "&blob=" + blobName;
    }
    try {
      BlobClient blobClient = getExistingBlob(orgNumber, blobName);
      BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
      OffsetDateTime expiry = OffsetDateTime.now().plus(validFor);
      BlobServiceSasSignatureValues sasValues =
          new BlobServiceSasSignatureValues(expiry, permission);
      return blobClient.getBlobUrl() + "?" + blobClient.generateSas(sasValues);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Azure SAS generation failed ({}), returning local URL", e.getMessage());
      return "/api/v1/files/download-local?orgNumber=" + orgNumber + "&blob=" + blobName;
    }
  }

  public void deleteFile(int orgNumber, String blobName) {
    if (!azureAvailable()) {
      deleteLocalFile(orgNumber, blobName);
      return;
    }
    try {
      BlobContainerClient container = getExistingContainer(orgNumber);
      container.getBlobClient(blobName).deleteIfExists();
    } catch (ResponseStatusException e) {
      if (e.getStatusCode().value() == 404) return; // already gone
      throw e;
    } catch (Exception e) {
      log.warn("Azure delete failed ({}), trying local", e.getMessage());
      deleteLocalFile(orgNumber, blobName);
    }
  }
}
