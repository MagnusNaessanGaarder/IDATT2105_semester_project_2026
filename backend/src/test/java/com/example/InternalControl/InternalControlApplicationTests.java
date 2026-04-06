package com.example.InternalControl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic Spring Boot context load test.
 * Verifies that the application context loads successfully.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class InternalControlApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that Spring context loads successfully
    }
}