package com.example.InternalControl.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing.
 * Configures thread pools for export job processing.

 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

  private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

  /**
   * Task executor for export jobs.
   * Uses a separate thread pool to prevent blocking main application threads.
   */
  @Bean(name = "exportTaskExecutor")
  public TaskExecutor exportTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("export-");
    executor.setRejectedExecutionHandler((r, exec) -> {
      log.warn("Export task queue is full. Task rejected.");
      throw new IllegalStateException("Export queue is full. Please try again later.");
    });
    executor.initialize();
    log.info("Export task executor initialized with corePoolSize=2, maxPoolSize=5");
    return executor;
  }

  @Override
  public Executor getAsyncExecutor() {
    return exportTaskExecutor();
  }
}
