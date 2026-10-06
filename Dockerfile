# =============================================================================
# Smart River Water Level Monitoring System — Dockerfile
# Day 19: Docker Containerization & Production Deployment
# =============================================================================
#
# Multi-Stage Build Pattern (Syllabus: UNIT V - Deployment & DevOps)
#   Stage 1 (builder) : Maven compiles source code and packages fat JAR
#   Stage 2 (runtime) : Lightweight JRE-only image runs the packaged JAR
#
# Benefits of Multi-Stage Build:
#   - Final image contains ONLY the JRE + JAR (no Maven, source code, or .m2 cache)
#   - Reduces image size from ~600MB (JDK + Maven) → ~150MB (JRE only)
#   - Improves security: no compiler or build tools in production image
#
# Build & Run:
#   docker build -t smart-river-system .
#   docker run -p 8080:8080 smart-river-system
#
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

LABEL stage="builder"
LABEL description="Maven build stage — compiles and packages the Spring Boot fat JAR"

# Set working directory inside container
WORKDIR /app

# Copy dependency manifest first (Docker layer caching optimization)
# If pom.xml doesn't change, Maven deps are restored from cache layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source code
COPY src ./src

# Package application (skip tests in build stage — tests run separately in CI)
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="Smart River Monitoring System — Day 19 Docker Deployment"
LABEL description="Production runtime: Spring Boot REST API + Web Dashboard"
LABEL version="0.20.0"

# Create non-root user for security best practice (principle of least privilege)
RUN addgroup -S riverapp && adduser -S riverapp -G riverapp

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create data directory for CSV + H2 database persistence
RUN mkdir -p /app/data /app/images /app/logs && \
    chown -R riverapp:riverapp /app

# Copy benchmark gauge images
COPY images/ ./images/

# Switch to non-root user
USER riverapp

# Expose Spring Boot default port
EXPOSE 8080

# JVM tuning for containerized environments:
#   -Xmx256m        : limit heap to 256MB (suits small/medium containers)
#   -Xms64m         : initial heap size
#   -XX:+UseG1GC    : G1 garbage collector (low-pause, good for web services)
#   -Djava.security.egd : fast entropy source for cryptographic operations (JWT)
ENV JAVA_OPTS="-Xmx256m -Xms64m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Health check — Docker will ping /api/stats every 30s to verify liveness
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/auth/info || exit 1

# Entrypoint: run Spring Boot JAR with JVM tuning options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
