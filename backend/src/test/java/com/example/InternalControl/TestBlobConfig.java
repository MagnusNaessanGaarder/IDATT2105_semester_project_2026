package com.example.InternalControl;

import com.azure.storage.blob.BlobServiceClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestBlobConfig {

  @Bean
  @Primary
  public BlobServiceClient blobServiceClient() {
    return Mockito.mock(BlobServiceClient.class);
  }
}