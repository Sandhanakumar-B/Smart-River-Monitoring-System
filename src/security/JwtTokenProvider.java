package security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 17: Spring Security & JWT Authentication
 * Syllabus Unit: UNIT V - API Security, JWT Token Lifecycle, Stateless Authentication
 *
 * Generates, signs, and validates JSON Web Tokens (JWT) for stateless API access.
 * Uses HMAC-SHA-256 signed tokens with configurable expiry for secure endpoint protection.
 *
 * Key Concepts Demonstrated:
 *   - JWT structure: Header.Payload.Signature (Base64 URL encoded)
 *   - HMAC-SHA-256 digital signature for tamper detection
 *   - Stateless authentication: no server-side session storage required
 *   - Claims-based identity: username and roles embedded in token payload
 */
@Component
public class JwtTokenProvider {

    // Configurable secret key (32+ bytes required for HS256)
    private static final String SECRET = "SmartRiverMonitoringSystem-JWT-SecretKey-Day17-SecurityModule-2026";
    private static final long   ACCESS_TOKEN_VALIDITY_MS  = 3600_000L;  // 1 hour
    private static final long   REFRESH_TOKEN_VALIDITY_MS = 86400_000L; // 24 hours

    // Derived signing key (HMAC-SHA-256)
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());

    // ======================================================================
    // Token Generation
    // ======================================================================

    /**
     * Generates a signed JWT access token embedding username and roles.
     *
     * @param username authenticated principal name
     * @param roles    list of authority/role strings (e.g., ROLE_ADMIN, ROLE_VIEWER)
     * @return compact JWT string (Header.Payload.Signature)
     */
    public String generateAccessToken(String username, List<String> roles) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY_MS);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a longer-lived JWT refresh token (no roles embedded).
     *
     * @param username authenticated principal name
     * @return compact JWT refresh token
     */
    public String generateRefreshToken(String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_VALIDITY_MS);

        return Jwts.builder()
                .subject(username)
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ======================================================================
    // Token Validation & Parsing
    // ======================================================================

    /**
     * Validates a JWT token signature, structure, and expiry.
     *
     * @param token compact JWT string
     * @return true if token is valid and not expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("[JWT] Token expired: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println("[JWT] Malformed token: " + e.getMessage());
        } catch (SignatureException e) {
            System.err.println("[JWT] Invalid signature: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("[JWT] Unsupported JWT type: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("[JWT] Token claims string is empty.");
        }
        return false;
    }

    /**
     * Extracts all JWT claims payload from a valid token.
     *
     * @param token compact JWT string
     * @return Claims object with all embedded payload fields
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the subject (username) from a JWT token.
     *
     * @param token compact JWT string
     * @return username (subject) claim
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the roles list from a JWT access token.
     *
     * @param token compact JWT string
     * @return list of role strings
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object rolesObj = extractClaims(token).get("roles");
        if (rolesObj instanceof List<?>) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }

    /**
     * Returns the token validity window in milliseconds.
     */
    public long getAccessTokenValidityMs() {
        return ACCESS_TOKEN_VALIDITY_MS;
    }

    /**
     * Checks if a token has expired.
     *
     * @param token compact JWT string
     * @return true if the token's expiry date is in the past
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiry = extractClaims(token).getExpiration();
            return expiry.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
