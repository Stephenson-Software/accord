package com.accordion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=TestSecretKeyForApplicationContextMinimum32BytesRequired",
    "jwt.expiration=86400000",
    // Never report test runs to the trace service.
    "usage-reporting.enabled=false"
})
class AccordionApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void testApplicationHasRequiredBeans() {
        assertNotNull(applicationContext.getBean("userService"));
        assertNotNull(applicationContext.getBean("chatService"));
        assertNotNull(applicationContext.getBean("userController"));
    }
}
