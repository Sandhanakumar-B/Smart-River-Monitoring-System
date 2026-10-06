package controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.JwtTokenProvider;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 17: Spring Security & JWT Authentication
 * Syllabus Unit: UNIT V - REST Auth Endpoint, AuthenticationManager, JWT Token Exchange
 *
 * Exposes authentication endpoints for the JWT login flow:
 *   POST /api/auth/login    - Submit credentials → receive JWT access + refresh tokens
 *   POST /api/auth/refresh  - Exchange refresh token → new access token
 *   GET  /api/auth/whoami   - Inspect current authenticated principal
 *
 * Authentication Flow:
 *   1. Client sends username + password to POST /api/auth/login
 *   2. AuthenticationManager validates credentials against UserDetailsService
 *   3. On success → JwtTokenProvider generates signed Access + Refresh tokens
 *   4. Client stores tokens, includes "Authorization: Bearer <accessToken>" on all future requests
 *   5. JwtAuthenticationFilter validates token on each request
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      jwtTokenProvider;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider      = jwtTokenProvider;
    }

    // ======================================================================
    // Login Request / Response DTOs (inner static classes)
    // ======================================================================

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public String accessToken;
        public String refreshToken;
        public String tokenType;
        public long   expiresInMs;
        public String username;
        public List<String> roles;

        public LoginResponse(String accessToken, String refreshToken, String username, List<String> roles, long expiresInMs) {
            this.accessToken  = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType    = "Bearer";
            this.expiresInMs  = expiresInMs;
            this.username     = username;
            this.roles        = roles;
        }
    }

    public static class RefreshRequest {
        public String refreshToken;
    }

    // ======================================================================
    // Authentication Endpoints
    // ======================================================================

    /**
     * POST /api/auth/login
     * Authenticates credentials and returns a signed JWT access + refresh token pair.
     *
     * Example body: { "username": "admin", "password": "admin123" }
     *
     * HTTP Responses:
     *   200 OK       - Login successful, token pair returned
     *   401 Unauthorized - Invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // AuthenticationManager delegates to UserDetailsService + PasswordEncoder
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username,
                            loginRequest.password
                    )
            );

            // Extract role names from GrantedAuthority list
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            // Generate signed JWT tokens
            String accessToken  = jwtTokenProvider.generateAccessToken(loginRequest.username, roles);
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginRequest.username);

            LoginResponse response = new LoginResponse(
                    accessToken,
                    refreshToken,
                    loginRequest.username,
                    roles,
                    jwtTokenProvider.getAccessTokenValidityMs()
            );

            System.out.println("[AUTH] Login successful for user: " + loginRequest.username + " | Roles: " + roles);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            System.err.println("[AUTH] Login failed for user: " + loginRequest.username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "error", "Invalid credentials",
                        "message", "Username or password is incorrect. Please try again.",
                        "status", 401
                    ));
        }
    }

    /**
     * POST /api/auth/refresh
     * Exchanges a valid refresh token for a new access token (without re-login).
     * Implements the standard JWT refresh flow for long-lived sessions.
     *
     * Example body: { "refreshToken": "<jwt-refresh-token>" }
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody RefreshRequest refreshRequest) {
        try {
            if (!jwtTokenProvider.validateToken(refreshRequest.refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token is invalid or expired. Please login again."));
            }

            String username = jwtTokenProvider.extractUsername(refreshRequest.refreshToken);
            // For a new access token, we issue it with standard VIEWER role (no stored roles in refresh token)
            List<String> defaultRoles = List.of("ROLE_VIEWER");
            String newAccessToken = jwtTokenProvider.generateAccessToken(username, defaultRoles);

            return ResponseEntity.ok(Map.of(
                    "accessToken",  newAccessToken,
                    "tokenType",    "Bearer",
                    "expiresInMs",  jwtTokenProvider.getAccessTokenValidityMs(),
                    "username",     username,
                    "message",      "Access token refreshed successfully."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token refresh failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/auth/whoami
     * Returns details about the currently authenticated user (extracted from JWT).
     * Protected endpoint — requires a valid Bearer token.
     */
    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI(org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No authenticated session found."));
        }

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(Map.of(
                "username",    auth.getName(),
                "roles",       roles,
                "authenticated", true,
                "message",     "JWT authentication active. Token validated successfully."
        ));
    }

    /**
     * GET /api/auth/info
     * Returns information about the Day 17 authentication module (public).
     */
    @GetMapping("/info")
    public ResponseEntity<?> authInfo() {
        return ResponseEntity.ok(Map.of(
                "day",         "Day 17: Spring Security & JWT Authentication",
                "module",      "UNIT V - Secure REST API with Stateless JWT Tokens",
                "loginUrl",    "POST /api/auth/login",
                "refreshUrl",  "POST /api/auth/refresh",
                "whoamiUrl",   "GET  /api/auth/whoami",
                "demoUsers",   Map.of(
                        "admin",    "admin123 → ROLE_ADMIN (full access)",
                        "monitor",  "monitor123 → ROLE_VIEWER (read-only)",
                        "operator", "operator123 → ROLE_ADMIN + ROLE_VIEWER"
                ),
                "tokenFormat", "Authorization: Bearer <access_token>",
                "algorithm",   "HMAC-SHA256 (HS256)",
                "expiry",      "Access: 1 hour | Refresh: 24 hours"
        ));
    }
}
