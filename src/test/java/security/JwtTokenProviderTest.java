package security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 18: Unit Testing with JUnit 5 & Mockito
 * Syllabus Unit: UNIT V - Security Module Unit Tests, JWT Token Lifecycle Testing
 *
 * Unit tests for JwtTokenProvider demonstrating:
 *   - Token generation and structural validation
 *   - Signature verification and tamper detection
 *   - Claims extraction (username, roles)
 *   - Expiry detection
 *   - Role isolation between access and refresh tokens
 */
@DisplayName("Day 18 – JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
    }

    @Test
    @DisplayName("generateAccessToken() produces a 3-part JWT")
    void generateAccessToken_shouldProduceThreePartJwt() {
        String token = provider.generateAccessToken("admin", List.of("ROLE_ADMIN"));

        assertNotNull(token, "Token must not be null");
        assertEquals(3, token.split("\\.").length, "JWT must have exactly 3 parts separated by dots");
    }

    @Test
    @DisplayName("validateToken() returns true for a freshly generated token")
    void validateToken_freshToken_shouldReturnTrue() {
        String token = provider.generateAccessToken("monitor", List.of("ROLE_VIEWER"));

        assertTrue(provider.validateToken(token), "Fresh token must be valid");
    }

    @Test
    @DisplayName("validateToken() returns false for a tampered token")
    void validateToken_tamperedSignature_shouldReturnFalse() {
        String token    = provider.generateAccessToken("admin", List.of("ROLE_ADMIN"));
        String tampered = token.substring(0, token.length() - 4) + "TAMPER";

        assertFalse(provider.validateToken(tampered), "Tampered token must be rejected");
    }

    @Test
    @DisplayName("validateToken() returns false for a completely invalid string")
    void validateToken_invalidString_shouldReturnFalse() {
        assertFalse(provider.validateToken("not.a.valid.jwt"), "Random string must be rejected");
        assertFalse(provider.validateToken(""), "Empty string must be rejected");
    }

    @Test
    @DisplayName("extractUsername() returns the correct subject from access token")
    void extractUsername_accessToken_shouldReturnCorrectUsername() {
        String token    = provider.generateAccessToken("operator", List.of("ROLE_ADMIN"));
        String username = provider.extractUsername(token);

        assertEquals("operator", username, "Extracted username must match the one used to generate token");
    }

    @Test
    @DisplayName("extractRoles() returns all roles embedded in access token")
    void extractRoles_accessTokenWithMultipleRoles_shouldReturnAllRoles() {
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_VIEWER");
        String token       = provider.generateAccessToken("operator", roles);

        List<String> extracted = provider.extractRoles(token);

        assertEquals(2, extracted.size(), "Must extract exactly 2 roles");
        assertTrue(extracted.containsAll(roles), "All roles must be extracted correctly");
    }

    @Test
    @DisplayName("generateRefreshToken() is valid and contains correct subject")
    void generateRefreshToken_shouldBeValidWithCorrectSubject() {
        String refreshToken = provider.generateRefreshToken("admin");

        assertTrue(provider.validateToken(refreshToken), "Refresh token must be valid");
        assertEquals("admin", provider.extractUsername(refreshToken), "Refresh subject must match");
    }

    @Test
    @DisplayName("isTokenExpired() returns false for fresh token")
    void isTokenExpired_freshToken_shouldReturnFalse() {
        String token = provider.generateAccessToken("admin", List.of("ROLE_ADMIN"));

        assertFalse(provider.isTokenExpired(token), "Fresh token must not be expired");
    }

    @Test
    @DisplayName("getAccessTokenValidityMs() returns a positive non-zero value")
    void getAccessTokenValidityMs_shouldReturnPositiveValue() {
        assertTrue(provider.getAccessTokenValidityMs() > 0, "Token validity must be a positive duration");
    }

    @Test
    @DisplayName("Viewer token must not contain ROLE_ADMIN")
    void viewerToken_shouldNotContainAdminRole() {
        String viewerToken       = provider.generateAccessToken("monitor", List.of("ROLE_VIEWER"));
        List<String> extractedRoles = provider.extractRoles(viewerToken);

        assertFalse(extractedRoles.contains("ROLE_ADMIN"), "Viewer token must not grant ADMIN role");
        assertTrue(extractedRoles.contains("ROLE_VIEWER"),  "Viewer token must contain ROLE_VIEWER");
    }
}
