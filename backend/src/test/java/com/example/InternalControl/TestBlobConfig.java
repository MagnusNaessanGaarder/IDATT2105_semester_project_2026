package com.example.InternalControl;

import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestBlobConfig {

  @Bean
  @Primary
  public BlobServiceClient blobServiceClient() {
    return new BlobServiceClientBuilder()
        .endpoint("http://127.0.0.1:10000/devstoreaccount1")
        .credential(new StorageSharedKeyCredential(
            "devstoreaccount1",
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq"
                + "/K1SZFPTOtr/KBHBeksoGMGw=="))
        .buildClient();
  }
}
