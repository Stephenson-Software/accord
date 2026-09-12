package com.accordion.security;

import com.accordion.dto.LoginRequest;
import com.accordion.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for security configuration.
 * Tests that security filters are properly configured and endpoints are protected as expected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.cors.allowed-origins=*",
    "app.username.max-length=50",
    "app.username.min-length=3",
    "app.password.min-length=8",
    "jwt.secret=TestSecretKeyForIntegrationTestsMinimum32BytesRequired",
    "jwt.expiration=86400000",
    // Never report test runs to the trace service.
    "usage-reporting.enabled=false"
})
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicEndpoints_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // Register endpoint should be accessible
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser123");
        registerRequest.setPassword("TestPass123");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Login endpoint should be accessible
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser123");
        loginRequest.setPassword("TestPass123");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_shouldReject_unauthenticatedRequests() throws Exception {
        // Messages endpoint should reject unauthenticated requests
        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isForbidden());

        // Channels endpoint should reject unauthenticated requests
        mockMvc.perform(get("/api/channels"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoints_shouldAccept_validJwtToken() throws Exception {
        // First, register a user and get a token
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("authuser456");
        registerRequest.setPassword("AuthPass456");

        MvcResult registerResult = mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = registerResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Now access protected endpoints with the token
        mockMvc.perform(get("/api/messages")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/channels")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_shouldReject_invalidJwtToken() throws Exception {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token";

        // Messages endpoint should reject invalid token
        mockMvc.perform(get("/api/messages")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isForbidden());

        // Channels endpoint should reject invalid token
        mockMvc.perform(get("/api/channels")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoints_shouldReject_invalidOrExpiredJwtToken() throws Exception {
        // This token has an invalid signature for the test secret
        // In practice, both invalid signatures and expired tokens will be rejected by JWT validation
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTU3NzgzNjgwMH0.abcdefghijklmnopqrstuvwxyz";

        // Should reject invalid or expired token
        mockMvc.perform(get("/api/messages")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoints_shouldReject_malformedAuthorizationHeader() throws Exception {
        // Missing "Bearer " prefix
        mockMvc.perform(get("/api/messages")
                .header("Authorization", "not-a-valid-format"))
                .andExpect(status().isForbidden());

        // Empty Authorization header
        mockMvc.perform(get("/api/messages")
                .header("Authorization", ""))
                .andExpect(status().isForbidden());
    }

    @Test
    void websocketEndpoint_shouldBePublic_forInitialHandshake() throws Exception {
        // WebSocket endpoint should be accessible without authentication
        // Note: Actual STOMP CONNECT will be authenticated by WebSocketAuthInterceptor
        // This test verifies the HTTP endpoint itself is public
        // SockJS returns 200 with a welcome message for GET requests
        mockMvc.perform(get("/ws"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome to SockJS")));
    }

    @Test
    void h2ConsoleEndpoint_shouldNotBeForbidden() throws Exception {
        // H2 console should be publicly accessible (though this should be disabled in production)
        // We verify it's not returning 403 Forbidden - the endpoint is permitted in security config
        // The actual response (200, 302, or 404) depends on H2 configuration
        MvcResult result = mockMvc.perform(get("/h2-console"))
                .andReturn();
        int status = result.getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertNotEquals(403, status, "H2 console should not be forbidden");
    }
}
