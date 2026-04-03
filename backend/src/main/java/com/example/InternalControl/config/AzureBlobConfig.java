package com.example.InternalControl.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for Azure Blob Storage client.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Configuration
@Profile("!test")
@Slf4j
public class AzureBlobConfig {

  @Value("${AZURE_STORAGE_CONNECTION_STRING:#{null}}")
  private String connectionString;

  @Bean
  public BlobServiceClient blobServiceClient() {
    if (connectionString == null || connectionString.isBlank()) {
      throw new IllegalStateException(
        "AZURE_STORAGE_CONNECTION_STRING is not set.\n" +
        "Add to .env: AZURE_STORAGE_CONNECTION_STRING=\"DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=...\""
      );
    }
    
    try {
      log.info("Initializing Azure Blob Storage client");
      return new BlobServiceClientBuilder()
          .connectionString(connectionString)
          .buildClient();
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
        "Invalid AZURE_STORAGE_CONNECTION_STRING format. " +
        "Ensure it starts with 'DefaultEndpointsProtocol=' and contains semicolons between parts.",
        e
      );
    }
  }
}
