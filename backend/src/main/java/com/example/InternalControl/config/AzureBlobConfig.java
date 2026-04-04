package com.example.InternalControl.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
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
    if (connectionString == null || connectionString.isBlank()) {
      throw new IllegalStateException("Azure connection string is not configured");
    }
    log.info("Creating Azure BlobServiceClient");
    return new BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient();
  }
}
