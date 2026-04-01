package com.example.InternalControl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class InternalControlApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that Spring context loads successfully
    }
}
