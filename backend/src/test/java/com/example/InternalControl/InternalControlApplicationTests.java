package com.example.InternalControl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestBlobConfig.class)
class InternalControlApplicationTests extends AbstractIntegrationTest  {

    @Test
    void contextLoads() {
        // Verifies that Spring context loads successfully
    }
}
