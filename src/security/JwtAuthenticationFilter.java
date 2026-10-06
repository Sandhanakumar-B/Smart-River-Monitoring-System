package security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System
 * Day 17: Spring Security & JWT Authentication
 * Syllabus Unit: UNIT V - Servlet Filter Chain, Security Context, Bearer Token Validation
 *
 * Intercepts every incoming HTTP request (once per request) to extract and validate
 * JWT Bearer tokens from the Authorization header.
 *
 * Servlet Filter Pipeline:
 *   HTTP Request -> JwtAuthenticationFilter -> Spring Security -> Controller
 *
 * Key Concepts Demonstrated:
 *   - OncePerRequestFilter: guaranteed single execution per HTTP request
 *   - Authorization header parsing: "Bearer <token>"
 *   - SecurityContextHolder: Spring Security's thread-local auth storage
 *   - UsernamePasswordAuthenticationToken: Spring's auth principal wrapper
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Core filter logic — executed once for every incoming HTTP request.
     * Validates Bearer JWT and populates Spring Security context if valid.
     *
     * @param request     incoming HTTP request
     * @param response    HTTP response
     * @param filterChain servlet filter chain (continues processing)
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractBearerToken(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                String username      = jwtTokenProvider.extractUsername(token);
                List<String> roles   = jwtTokenProvider.extractRoles(token);

                // Convert role strings to Spring GrantedAuthority objects
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Create Spring Security authentication token (pre-authenticated)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // Attach HTTP request details (IP, session) to authentication
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Store authentication in SecurityContextHolder (thread-local)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Do NOT break the filter chain on JWT errors — let Spring Security deny access
            System.err.println("[JWT Filter] Authentication error: " + ex.getMessage());
        }

        // Always continue the filter chain regardless of JWT validation outcome
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization: Bearer <token> header.
     *
     * @param request HTTP request
     * @return raw JWT string, or null if header is absent/malformed
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Strip "Bearer " prefix
        }
        return null;
    }
}
