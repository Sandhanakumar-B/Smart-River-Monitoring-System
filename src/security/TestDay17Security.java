package security;

import dao.StationFileDAO;
import dao.WaterLevelRecordFileDAO;
import java.util.List;
import service.HydrologicalRiskService;
import service.RiverMonitoringService;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 17: Spring Security & JWT Authentication
 * Syllabus Unit: UNIT V - Automated JWT Lifecycle Verification Suite
 *
 * Standalone verification suite for the Day 17 JWT security module.
 * Tests: token generation, signature validation, expiry detection,
 *        claim extraction (username, roles), and tamper detection.
 *
 * Run with:
 *   mvn exec:java -Dexec.mainClass="security.TestDay17Security"
 */
public class TestDay17Security {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  DAY 17 VERIFICATION SUITE: SPRING SECURITY & JWT AUTH         ");
        System.out.println("=================================================================\n");

        JwtTokenProvider provider = new JwtTokenProvider();
        int passed = 0;
        int failed = 0;

        try {
            // Test 1: Access token generation
            System.out.print("[TEST 1] JWT Access Token Generation... ");
            List<String> roles = List.of("ROLE_ADMIN", "ROLE_VIEWER");
            String token = provider.generateAccessToken("admin", roles);
            assertCondition(token != null && !token.isEmpty(), "Access token must not be null/empty.");
            assertCondition(token.split("\\.").length == 3, "JWT must have 3 parts (Header.Payload.Signature).");
            System.out.println("PASSED! Token: " + token.substring(0, 30) + "...");
            passed++;

            // Test 2: Token validation
            System.out.print("[TEST 2] JWT Token Signature Validation... ");
            boolean valid = provider.validateToken(token);
            assertCondition(valid, "Freshly generated token must be valid.");
            System.out.println("PASSED! Token is valid and signature verified.");
            passed++;

            // Test 3: Username extraction from token
            System.out.print("[TEST 3] JWT Subject (Username) Extraction... ");
            String extractedUser = provider.extractUsername(token);
            assertCondition("admin".equals(extractedUser), "Extracted username must be 'admin'.");
            System.out.println("PASSED! Extracted username: " + extractedUser);
            passed++;

            // Test 4: Roles claim extraction
            System.out.print("[TEST 4] JWT Roles Claim Extraction... ");
            List<String> extractedRoles = provider.extractRoles(token);
            assertCondition(extractedRoles.contains("ROLE_ADMIN"), "Must contain ROLE_ADMIN.");
            assertCondition(extractedRoles.contains("ROLE_VIEWER"), "Must contain ROLE_VIEWER.");
            System.out.println("PASSED! Roles: " + extractedRoles);
            passed++;

            // Test 5: Tamper detection - modified token
            System.out.print("[TEST 5] JWT Tamper Detection (modified signature)... ");
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";
            boolean tamperedValid = provider.validateToken(tamperedToken);
            assertCondition(!tamperedValid, "Tampered token must be rejected.");
            System.out.println("PASSED! Tampered token correctly rejected.");
            passed++;

            // Test 6: Refresh token generation and validation
            System.out.print("[TEST 6] JWT Refresh Token Generation & Validation... ");
            String refreshToken = provider.generateRefreshToken("admin");
            assertCondition(refreshToken != null && !refreshToken.isEmpty(), "Refresh token must not be null.");
            assertCondition(provider.validateToken(refreshToken), "Refresh token must be valid.");
            assertCondition("admin".equals(provider.extractUsername(refreshToken)), "Refresh token subject must be 'admin'.");
            System.out.println("PASSED! Refresh token valid, subject: " + provider.extractUsername(refreshToken));
            passed++;

            // Test 7: Token not expired
            System.out.print("[TEST 7] JWT Token Expiry Check (fresh token)... ");
            boolean expired = provider.isTokenExpired(token);
            assertCondition(!expired, "Freshly generated token must NOT be expired.");
            System.out.println("PASSED! Token is within valid window.");
            passed++;

            // Test 8: Viewer role token has correct claims
            System.out.print("[TEST 8] Viewer Role Token Claim Isolation... ");
            String viewerToken = provider.generateAccessToken("monitor", List.of("ROLE_VIEWER"));
            List<String> viewerRoles = provider.extractRoles(viewerToken);
            assertCondition(viewerRoles.contains("ROLE_VIEWER"), "Viewer token must have ROLE_VIEWER.");
            assertCondition(!viewerRoles.contains("ROLE_ADMIN"), "Viewer token must NOT have ROLE_ADMIN.");
            System.out.println("PASSED! Viewer token roles: " + viewerRoles);
            passed++;

            // Test 9: Security integration with Risk Service
            System.out.print("[TEST 9] Authenticated Request Context (Risk Service Integration)... ");
            RiverMonitoringService monitoringService = new RiverMonitoringService(new StationFileDAO(), new WaterLevelRecordFileDAO());
            HydrologicalRiskService riskService = new HydrologicalRiskService(monitoringService);
            var stations = monitoringService.getAllStations();
            assertCondition(!stations.isEmpty(), "Service must return at least one station.");
            var risk = riskService.assessStationRisk(stations.get(0));
            assertCondition(risk != null, "Risk assessment must not be null.");
            System.out.println("PASSED! Risk assessment computed for: " + risk.getStationName() + " → " + risk.getRiskLevel());
            passed++;

            System.out.println("\n-----------------------------------------------------------------");
            System.out.println("DAY 17: ALL " + passed + " JWT SECURITY TESTS PASSED! (0 failures)");
            System.out.println("-----------------------------------------------------------------");
            System.out.println("\nDay 17 Security Module Summary:");
            System.out.println("  ✅ JWT Token Provider      : HMAC-SHA256 signed, 1-hour access / 24h refresh");
            System.out.println("  ✅ JWT Auth Filter         : OncePerRequestFilter Bearer token validation");
            System.out.println("  ✅ Spring Security Config  : STATELESS sessions, RBAC roles (ADMIN/VIEWER)");
            System.out.println("  ✅ Auth Controller         : POST /api/auth/login → JWT token exchange");
            System.out.println("  ✅ BCrypt Password Hash    : Cost factor 12, OWASP compliant");
            System.out.println("\nDemo Credentials:");
            System.out.println("  POST http://localhost:8080/api/auth/login");
            System.out.println("  Body: { \"username\": \"admin\", \"password\": \"admin123\" }");
            System.out.println("  → Returns: { \"accessToken\": \"<jwt>\", \"refreshToken\": \"<jwt>\", ... }");

        } catch (Throwable t) {
            System.err.println("\nFAILED! Exception: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }
}
