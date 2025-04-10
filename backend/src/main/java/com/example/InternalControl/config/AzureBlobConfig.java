package com.example.InternalControl.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.policy.RequestRetryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@Slf4j
public class AzureBlobConfig {

  @Value("${AZURE_STORAGE_CONNECTION_STRING:}")
  private String connectionString;

  @Bean
  public BlobServiceClient blobServiceClient() {
    if (connectionString == null || connectionString.isBlank() || connectionString.equals("CHANGE_ME")) {
      log.warn("Azure Storage connection string is not configured. Document storage will be unavailable.");
      return null;
    }
    try {
      log.info("Creating Azure BlobServiceClient");
      // Fail fast: 1 try, 6-second timeout per operation — lets the local fallback kick in quickly
      RequestRetryOptions retryOptions = new RequestRetryOptions(null, 1, 6, null, null, null);
      return new BlobServiceClientBuilder()
          .connectionString(connectionString)
          .retryOptions(retryOptions)
          .buildClient();
    } catch (Exception e) {
      log.error("Failed to create Azure BlobServiceClient: {}", e.getMessage());
      return null;
    }
  }
}
