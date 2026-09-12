package com.accordion.controller;

import com.accordion.model.Channel;
import com.accordion.model.TypingIndicator;
import com.accordion.service.ChannelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=TestSecretKeyForTypingIndicatorMinimum32BytesRequired",
    "jwt.expiration=86400000",
    // Never report test runs to the trace service.
    "usage-reporting.enabled=false"
})
public class TypingIndicatorTest {

    @Autowired
    private ChatController chatController;

    @MockBean
    private ChannelService channelService;

    @Test
    public void testUserTyping_ValidPayload() {
        // Arrange
        Long channelId = 1L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "testuser");
        payload.put("typing", true);

        Channel mockChannel = new Channel();
        mockChannel.setId(channelId);
        mockChannel.setName("general");
        when(channelService.getChannelById(channelId)).thenReturn(Optional.of(mockChannel));

        // Act
        TypingIndicator result = chatController.userTyping(channelId, payload);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(channelId, result.getChannelId());
        assertTrue(result.isTyping());
    }

    @Test
    public void testUserTyping_TypingFalse() {
        // Arrange
        Long channelId = 1L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "testuser");
        payload.put("typing", false);

        Channel mockChannel = new Channel();
        mockChannel.setId(channelId);
        mockChannel.setName("general");
        when(channelService.getChannelById(channelId)).thenReturn(Optional.of(mockChannel));

        // Act
        TypingIndicator result = chatController.userTyping(channelId, payload);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertFalse(result.isTyping());
    }

    @Test
    public void testUserTyping_DefaultsToTrue() {
        // Arrange
        Long channelId = 1L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "testuser");
        // No "typing" field provided

        Channel mockChannel = new Channel();
        mockChannel.setId(channelId);
        mockChannel.setName("general");
        when(channelService.getChannelById(channelId)).thenReturn(Optional.of(mockChannel));

        // Act
        TypingIndicator result = chatController.userTyping(channelId, payload);

        // Assert
        assertNotNull(result);
        assertTrue(result.isTyping());
    }

    @Test
    public void testUserTyping_TrimsUsername() {
        // Arrange
        Long channelId = 1L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "  testuser  ");
        payload.put("typing", true);

        Channel mockChannel = new Channel();
        mockChannel.setId(channelId);
        mockChannel.setName("general");
        when(channelService.getChannelById(channelId)).thenReturn(Optional.of(mockChannel));

        // Act
        TypingIndicator result = chatController.userTyping(channelId, payload);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    public void testUserTyping_NullPayload() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            chatController.userTyping(1L, null);
        });
    }

    @Test
    public void testUserTyping_EmptyUsername() {
        // Arrange
        Long channelId = 1L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "");
        payload.put("typing", true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            chatController.userTyping(channelId, payload);
        });
    }

    @Test
    public void testUserTyping_WhitespaceOnlyUsername() {
        // Arrange
        Long channelId = 1L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "   ");
        payload.put("typing", true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            chatController.userTyping(channelId, payload);
        });
    }

    @Test
    public void testUserTyping_ChannelNotFound() {
        // Arrange
        Long channelId = 999L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "testuser");
        payload.put("typing", true);

        when(channelService.getChannelById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            chatController.userTyping(channelId, payload);
        });
    }
}
