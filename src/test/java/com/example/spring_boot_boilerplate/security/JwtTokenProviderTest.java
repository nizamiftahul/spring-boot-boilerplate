package com.example.spring_boot_boilerplate.security;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.SignatureException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JwtTokenProvider.
 * Validates token creation, parsing, and validation logic.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "testSecretKeyForJwtTokenProviderUnitTestWith32Bytes";
    private static final long TEST_EXPIRATION_MS = 15 * 60 * 1000; // 15 minutes

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, TEST_EXPIRATION_MS);
    }

    @Test
    @DisplayName("Create access token with valid username and roles")
    void testCreateAccessToken() {
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");

        String token = jwtTokenProvider.createAccessToken(username, roles);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Extract username from valid token")
    void testGetUsername() {
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_USER");
        String token = jwtTokenProvider.createAccessToken(username, roles);

        String extractedUsername = jwtTokenProvider.getUsername(token);

        assertEquals(username, extractedUsername);
    }

    @Test
    @DisplayName("Extract roles from valid token")
    void testGetRoles() {
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");
        String token = jwtTokenProvider.createAccessToken(username, roles);

        List<String> extractedRoles = jwtTokenProvider.getRoles(token);

        assertEquals(roles, extractedRoles);
    }

    @Test
    @DisplayName("Validate token returns false for invalid token")
    void testValidateInvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Validate token returns false for tampered token")
    void testValidateTamperedToken() {
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_USER");
        String token = jwtTokenProvider.createAccessToken(username, roles);

        // Tamper with token by modifying last character
        String tamperedToken = token.substring(0, token.length() - 1) + "X";

        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Create token with empty roles")
    void testCreateAccessTokenEmptyRoles() {
        String username = "testuser";
        List<String> roles = Arrays.asList();

        String token = jwtTokenProvider.createAccessToken(username, roles);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Token contains correct username and roles in claims")
    void testTokenClaimsIntegrity() {
        String username = "admin";
        List<String> roles = Arrays.asList("ROLE_ADMIN", "ROLE_MODERATOR");
        String token = jwtTokenProvider.createAccessToken(username, roles);

        assertEquals(username, jwtTokenProvider.getUsername(token));
        assertEquals(roles, jwtTokenProvider.getRoles(token));
    }
}
