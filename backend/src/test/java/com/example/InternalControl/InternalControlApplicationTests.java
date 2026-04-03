package com.example.InternalControl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    locations = "classpath:application-test.yml",
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    }
)
@Import(TestBlobConfig.class)
class InternalControlApplicationTests extends AbstractIntegrationTest {
    @Test
    void contextLoads() {
        // Verifies that Spring context loads successfully
    }
}
