package com.example.InternalControl;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton shared MySQL container for all tests.
 * This is more efficient than starting a new container for each test class.
 */
public class SharedMySQLContainer extends MySQLContainer<SharedMySQLContainer> {
    
    private static final DockerImageName IMAGE = DockerImageName.parse("mysql:8.0");
    private static SharedMySQLContainer container;
    
    private SharedMySQLContainer() {
        super(IMAGE);
        withDatabaseName("testdb");
        withUsername("test");
        withPassword("test");
        withReuse(true); // Enable container reuse
    }
    
    public static SharedMySQLContainer getInstance() {
        if (container == null) {
            container = new SharedMySQLContainer();
        }
        return container;
    }
    
    @Override
    public void start() {
        if (!isRunning()) {
            super.start();
        }
    }
    
    @Override
    public void stop() {
        // Do not stop between tests - container is reused
    }
    
    public static void stopContainer() {
        if (container != null && container.isRunning()) {
            container.superStop();
        }
    }
    
    private void superStop() {
        super.stop();
    }
}
