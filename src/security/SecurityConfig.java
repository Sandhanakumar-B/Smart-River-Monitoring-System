package security;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 17: Spring Security & JWT Authentication
 * Syllabus Unit: UNIT V - Spring Security Configuration, Role-Based Access Control, RBAC
 *
 * Configures the Spring Security filter chain with JWT-based stateless authentication,
 * role-based access control (RBAC), and BCrypt password hashing.
 *
 * Security Architecture:
 *   Public Endpoints  : POST /api/auth/**  (login, register)
 *                       GET  /api/public/** (open telemetry streams)
 *                       GET  /**            (web dashboard HTML/CSS/JS)
 *   Protected READ    : ROLE_VIEWER, ROLE_ADMIN  (GET /api/stations, /api/readings, /api/stats)
 *   Protected WRITE   : ROLE_ADMIN only          (POST, DELETE /api/stations, /api/readings)
 *
 * Key Concepts Demonstrated:
 *   - @EnableWebSecurity: activates Spring Security auto-configuration
 *   - SecurityFilterChain: replaces legacy WebSecurityConfigurerAdapter pattern
 *   - STATELESS session: no HttpSession — each request must carry JWT
 *   - BCryptPasswordEncoder: one-way adaptive hashing (cost factor 12)
 *   - Role-Based Access Control (RBAC): ROLE_ADMIN vs ROLE_VIEWER
 *   - InMemoryUserDetailsManager: demo user registry (production: DB-backed)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Defines the security filter chain — Spring Security's main configuration DSL.
     * Replaces the deprecated WebSecurityConfigurerAdapter.configure(HttpSecurity).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — appropriate for stateless REST APIs with JWT
            .csrf(csrf -> csrf.disable())

            // CORS — permit all for development (restrict in production)
            .cors(cors -> cors.disable())

            // Session management: STATELESS — no HttpSession storage
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules (order matters — more specific first)
            .authorizeHttpRequests(authz -> authz

                // Public: authentication endpoints (login/register)
                .requestMatchers("/api/auth/**").permitAll()

                // Public: static web dashboard assets (HTML/CSS/JS)
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico").permitAll()

                // Public: H2 console (development only)
                .requestMatchers("/h2-console/**").permitAll()

                // Public: SSE stream endpoint (web dashboard reads without auth)
                .requestMatchers("/api/stream/telemetry").permitAll()

                // Read-only endpoints: accessible by VIEWER or ADMIN roles
                .requestMatchers(HttpMethod.GET, "/api/stations", "/api/readings/**",
                                 "/api/stats", "/api/basin/risk", "/api/stream/status").hasAnyRole("VIEWER", "ADMIN")

                // Risk endpoints: readable by any authenticated user
                .requestMatchers(HttpMethod.GET, "/api/stations/*/risk").hasAnyRole("VIEWER", "ADMIN")

                // Write endpoints: ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/stations", "/api/readings",
                                 "/api/simulation/**", "/api/stream/simulation/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/stations/**").hasRole("ADMIN")

                // All other API requests require authentication
                .anyRequest().authenticated()
            )

            // Disable X-Frame-Options to allow H2 console in iframe (dev only)
            .headers(headers -> headers.frameOptions(fo -> fo.disable()))

            // Insert JWT filter BEFORE Spring's default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * In-memory user registry — demonstrates Spring Security UserDetails API.
     * In production, replace with JPA-backed UserDetailsService + database.
     *
     * Users:
     *   admin   / admin123  → ROLE_ADMIN  (full read-write access)
     *   monitor / monitor123 → ROLE_VIEWER (read-only access)
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        UserDetails monitor = User.builder()
                .username("monitor")
                .password(encoder.encode("monitor123"))
                .roles("VIEWER")
                .build();

        UserDetails operator = User.builder()
                .username("operator")
                .password(encoder.encode("operator123"))
                .roles("ADMIN", "VIEWER")
                .build();

        return new InMemoryUserDetailsManager(List.of(admin, monitor, operator));
    }

    /**
     * BCrypt password encoder — adaptive one-way hashing with cost factor 12.
     * BCrypt is the recommended standard for password storage (OWASP compliant).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationManager bean — required for programmatic login in AuthController.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
