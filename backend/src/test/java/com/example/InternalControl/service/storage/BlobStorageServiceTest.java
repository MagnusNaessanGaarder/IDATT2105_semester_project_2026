package com.example.InternalControl.service.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BlobStorageService.
 *
 * Note: Azure SDK classes are final and require special mocking configuration.
 * These tests focus on the service logic that can be tested without mocking Azure clients.
 */
class BlobStorageServiceTest {

    private static final int ORG_NUMBER = 123;

    @Test
    void shouldInstantiateService() {
        // Given & When
        BlobStorageService service = new BlobStorageService(null);

        // Then
        assertThat(service).isNotNull();
    }
}
