package com.accordion.config;

import com.accordion.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=TestSecretKeyForApplicationContextMinimum32BytesRequired",
    "jwt.expiration=86400000",
    // Never report test runs to the trace service.
    "usage-reporting.enabled=false"
})
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testTimestampSerializedAsIso8601String() throws Exception {
        ChatMessage message = new ChatMessage("user", "hello");
        String json = objectMapper.writeValueAsString(message);

        // Timestamp should be a JSON string, not an array
        assertTrue(json.contains("\"timestamp\":\""),
            "Timestamp should be serialized as an ISO-8601 string, not an array. Got: " + json);

        // Should NOT contain array format like [2026,2,15,...]
        assertFalse(json.contains("\"timestamp\":["),
            "Timestamp should not be serialized as an array. Got: " + json);
    }

    @Test
    void testTimestampStringIsIso8601Format() throws Exception {
        LocalDateTime knownTime = LocalDateTime.of(2026, 2, 15, 14, 30, 45);
        ChatMessage message = new ChatMessage("user", "hello");
        message.setTimestamp(knownTime);

        String json = objectMapper.writeValueAsString(message);

        // Should contain the date in ISO-8601 format
        assertTrue(json.contains("2026-02-15T14:30:45"),
            "Timestamp should use ISO-8601 format. Got: " + json);
    }

    @Test
    void testTimestampCanBeRoundTripped() throws Exception {
        LocalDateTime original = LocalDateTime.of(2026, 2, 15, 14, 30, 45);
        ChatMessage message = new ChatMessage("user", "hello");
        message.setTimestamp(original);

        String json = objectMapper.writeValueAsString(message);
        ChatMessage deserialized = objectMapper.readValue(json, ChatMessage.class);

        assertEquals(original, deserialized.getTimestamp(),
            "Timestamp should survive a serialize/deserialize round trip");
    }
}
