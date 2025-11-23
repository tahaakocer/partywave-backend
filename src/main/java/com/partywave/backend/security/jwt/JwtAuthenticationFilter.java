package com.partywave.backend.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT Authentication Filter for PartyWave application.
 * Intercepts HTTP requests and validates JWT tokens from Authorization header.
 *
 * Filter Flow:
 * 1. Extract JWT token from "Authorization: Bearer <token>" header
 * 2. Validate token using JwtTokenProvider
 * 3. If valid, set authentication in SecurityContext
 * 4. If invalid, continue without authentication (let Spring Security handle 401)
 *
 * This filter runs before Spring Security's authorization checks.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Filter method that processes each HTTP request.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Extract JWT token from request
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && !jwt.isEmpty()) {
                LOG.debug("JWT token found in request: {}", request.getRequestURI());

                try {
                    // Validate token
                    Jwt validatedJwt = jwtTokenProvider.validateToken(jwt);

                    // Extract user details from token
                    String userId = validatedJwt.getSubject();
                    String email = jwtTokenProvider.getEmailFromToken(validatedJwt);

                    LOG.debug("Token validated for user: {} ({})", userId, email);

                    // Create authentication object
                    // Note: We use email as principal for Spring Security compatibility
                    // The actual user ID is available from the Jwt object
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        validatedJwt, // Principal is the JWT itself
                        jwt, // Credentials is the token string
                        java.util.Collections.emptyList() // Authorities (can be added later if needed)
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    LOG.debug("Authentication set in SecurityContext for user: {}", userId);
                } catch (Exception e) {
                    LOG.warn("JWT validation failed: {}", e.getMessage());
                    // Don't set authentication - let Spring Security handle 401
                    SecurityContextHolder.clearContext();
                }
            } else {
                LOG.debug("No JWT token found in request: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            LOG.error("Error processing JWT authentication: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     *
     * @param request HTTP request
     * @return JWT token string, or null if not found
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Determine if this filter should be applied to the current request.
     * This implementation applies the filter to all requests.
     *
     * @param request HTTP request
     * @return true if filter should be applied
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Apply filter to all requests
        // Spring Security's filter chain will handle public endpoints
        return false;
    }
}
