package com.example.InternalControl.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@Slf4j
public class AzureBlobConfig {

  private static final Logger log = LoggerFactory.getLogger(AzureBlobConfig.class);

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
      return new BlobServiceClientBuilder()
          .connectionString(connectionString)
          .buildClient();
    } catch (Exception e) {
      log.error("Failed to create Azure BlobServiceClient: {}", e.getMessage());
      return null;
    }
  }
}
