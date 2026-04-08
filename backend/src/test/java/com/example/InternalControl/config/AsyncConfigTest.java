package com.example.InternalControl.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AsyncConfig.
 * Verifies asynchronous task executor configuration.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest
@TestPropertySource(
        locations = "classpath:application-test.properties",
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@DisplayName("AsyncConfig Tests")
class AsyncConfigTest {

    @Autowired
    @Qualifier("exportTaskExecutor")
    private TaskExecutor exportTaskExecutor;

    @Autowired
    private AsyncConfig asyncConfig;

    @Test
    @DisplayName("Should configure export task executor")
    void shouldConfigureExportTaskExecutor() {
        // Then
        assertThat(exportTaskExecutor).isNotNull();
        assertThat(exportTaskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @Test
    @DisplayName("Should configure executor with correct pool settings")
    void shouldConfigureExecutorWithCorrectPoolSettings() {
        // Given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) exportTaskExecutor;

        // Then
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(5);
        assertThat(executor.getQueueCapacity()).isEqualTo(100);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("export-");
    }

    @Test
    @DisplayName("Should provide async executor")
    void shouldProvideAsyncExecutor() {
        // When
        Executor asyncExecutor = asyncConfig.getAsyncExecutor();

        // Then
        assertThat(asyncExecutor).isNotNull();
        assertThat(asyncExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @Test
    @DisplayName("Should use same executor for async and export")
    void shouldUseSameExecutorForAsyncAndExport() {
        // When
        Executor asyncExecutor = asyncConfig.getAsyncExecutor();

        // Then
        assertThat(asyncExecutor).isSameAs(exportTaskExecutor);
    }
}
