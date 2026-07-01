package com.example.spring_boot_boilerplate.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter.
 * 
 * Intercepts each request once (OncePerRequestFilter) to extract and validate
 * JWT tokens from Authorization: Bearer header. Sets SecurityContext for downstream
 * components.
 * 
 * Single Responsibility: Extract and validate JWT from HTTP requests.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor injection of JwtTokenProvider (Dependency Inversion).
     * Depends on abstraction (JwtTokenProvider component) not concrete classes.
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Filter execution: extract JWT, validate, and set SecurityContext.
     * 
     * Execution flow:
     * 1. Extract JWT from Authorization header
     * 2. Validate token signature and expiration
     * 3. Extract username and roles from token
     * 4. Create Authentication token
     * 5. Set SecurityContext
     * 6. Continue filter chain
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsername(jwt);
                List<String> roles = jwtTokenProvider.getRoles(jwt);

                // Convert roles to GrantedAuthority for Spring Security
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Create authentication token with authorities
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.debug("JWT authentication set for user: {}", username);
            }
        } catch (Exception e) {
            logger.error("Failed to set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization: Bearer <token> header.
     * 
     * @param request the HTTP request
     * @return JWT token string or null if not found/invalid format
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
