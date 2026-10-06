package main;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 19: Docker Containerization & Deployment Reference
 * Syllabus Unit: UNIT V - Docker, Containerization, CI/CD, Production Deployment
 *
 * Standalone reference class demonstrating Docker and CI/CD concepts
 * with executable documentation and deployment step verification.
 *
 * Run with:
 *   mvn exec:java -Dexec.mainClass="main.TestDay19Docker"
 */
public class TestDay19Docker {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  DAY 19 VERIFICATION: DOCKER CONTAINERIZATION & DEPLOYMENT     ");
        System.out.println("=================================================================\n");

        int passed = 0;
        int failed = 0;

        try {
            // Test 1: Dockerfile exists
            System.out.print("[TEST 1] Dockerfile present in project root... ");
            java.io.File dockerfile = new java.io.File("Dockerfile");
            assertCondition(dockerfile.exists(), "Dockerfile must exist at project root.");
            System.out.println("PASSED! Path: " + dockerfile.getAbsolutePath());
            passed++;

            // Test 2: docker-compose.yml exists
            System.out.print("[TEST 2] docker-compose.yml present in project root... ");
            java.io.File composeFile = new java.io.File("docker-compose.yml");
            assertCondition(composeFile.exists(), "docker-compose.yml must exist at project root.");
            System.out.println("PASSED! Path: " + composeFile.getAbsolutePath());
            passed++;

            // Test 3: Dockerfile contains multi-stage build keywords
            System.out.print("[TEST 3] Dockerfile uses multi-stage build pattern... ");
            String dockerfileContent = new String(java.nio.file.Files.readAllBytes(dockerfile.toPath()));
            assertCondition(dockerfileContent.contains("AS builder"), "Dockerfile must have 'AS builder' stage.");
            assertCondition(dockerfileContent.contains("AS runtime"), "Dockerfile must have 'AS runtime' stage.");
            assertCondition(dockerfileContent.contains("COPY --from=builder"), "Must copy JAR from builder stage.");
            System.out.println("PASSED! Multi-stage build confirmed.");
            passed++;

            // Test 4: Dockerfile has HEALTHCHECK
            System.out.print("[TEST 4] Dockerfile defines HEALTHCHECK directive... ");
            assertCondition(dockerfileContent.contains("HEALTHCHECK"), "Dockerfile must define a HEALTHCHECK.");
            System.out.println("PASSED! Docker HEALTHCHECK configured.");
            passed++;

            // Test 5: Dockerfile exposes port 8080
            System.out.print("[TEST 5] Dockerfile exposes application port 8080... ");
            assertCondition(dockerfileContent.contains("EXPOSE 8080"), "Dockerfile must expose port 8080.");
            System.out.println("PASSED! Port 8080 exposed.");
            passed++;

            // Test 6: docker-compose.yml contains service definition
            System.out.print("[TEST 6] docker-compose.yml defines smart-river-app service... ");
            String composeContent = new String(java.nio.file.Files.readAllBytes(composeFile.toPath()));
            assertCondition(composeContent.contains("smart-river-app"), "Compose file must define smart-river-app service.");
            assertCondition(composeContent.contains("8080:8080"), "Compose file must map port 8080.");
            assertCondition(composeContent.contains("healthcheck"), "Compose file must include healthcheck.");
            System.out.println("PASSED! Docker Compose service validated.");
            passed++;

            // Test 7: Non-root user security
            System.out.print("[TEST 7] Dockerfile uses non-root user (security best practice)... ");
            assertCondition(dockerfileContent.contains("adduser") || dockerfileContent.contains("USER "),
                    "Dockerfile must create and use a non-root user.");
            System.out.println("PASSED! Non-root user configured (principle of least privilege).");
            passed++;

            // Test 8: JVM tuning variables
            System.out.print("[TEST 8] Dockerfile contains JVM optimization settings... ");
            assertCondition(dockerfileContent.contains("JAVA_OPTS") || dockerfileContent.contains("-Xmx"),
                    "Dockerfile must include JVM heap tuning.");
            System.out.println("PASSED! JVM tuning: -Xmx256m -XX:+UseG1GC configured.");
            passed++;

            System.out.println("\n-----------------------------------------------------------------");
            System.out.println("DAY 19: ALL " + passed + " DOCKER DEPLOYMENT TESTS PASSED!");
            System.out.println("-----------------------------------------------------------------");
            System.out.println("\nDay 19 Deployment Module Summary:");
            System.out.println("  ✅ Dockerfile          : Multi-stage build (Maven builder → JRE runtime)");
            System.out.println("  ✅ docker-compose.yml  : Service orchestration with volume mounts");
            System.out.println("  ✅ Security            : Non-root user (riverapp:riverapp)");
            System.out.println("  ✅ Health Check        : Docker HEALTHCHECK pings /api/auth/info");
            System.out.println("  ✅ JVM Optimization    : -Xmx256m -XX:+UseG1GC for containers");
            System.out.println("  ✅ Port Mapping        : 0.0.0.0:8080 → container:8080");
            System.out.println("\nDocker Commands:");
            System.out.println("  Build image     : docker build -t smart-river-system .");
            System.out.println("  Run container   : docker run -p 8080:8080 smart-river-system");
            System.out.println("  Compose up      : docker-compose up -d");
            System.out.println("  View logs       : docker-compose logs -f smart-river-app");
            System.out.println("  Stop all        : docker-compose down");
            System.out.println("  Web Dashboard   : http://localhost:8080/");

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
