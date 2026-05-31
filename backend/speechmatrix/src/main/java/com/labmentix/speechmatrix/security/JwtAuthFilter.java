package com.labmentix.speechmatrix.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Runs once per request.
 * Extracts the JWT from the Authorization header, validates it,
 * loads the user, and sets authentication in the SecurityContext.
 *
 * Edge cases handled:
 *  - Missing / malformed Authorization header    â†’ pass through (401 from EntryPoint)
 *  - Invalid / expired / blacklisted token       â†’ 401 JSON inline
 *  - Valid token but user deleted from DB        â†’ 401 JSON inline
 *  - Any unexpected exception                    â†’ 500 JSON inline, never stack trace
 */
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired private JwtUtils                jwtUtils;
    @Autowired private UserDetailsServiceImpl  userDetailsService;

    // â”€â”€ Public routes that never need a token â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static final String[] PUBLIC_PATHS = {
        "/api/auth/register",
        "/api/auth/login",
        "/actuator/health",
        "/ws/"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pub : PUBLIC_PATHS) {
            if (path.startsWith(pub)) {
                log.trace("JWT filter skipped for public path: {}", path);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("JWT filter â†’ {} {}", request.getMethod(), path);

        try {
            String jwt = extractToken(request);

            // No token present â€” let Spring Security handle it (401 via EntryPoint)
            if (jwt == null) {
                log.trace("No Bearer token found for: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // Token present but invalid / expired / blacklisted
            if (!jwtUtils.validateToken(jwt)) {
                log.warn("Invalid or expired token for path: {}", path);
                writeUnauthorized(response, "Token is invalid, expired, or has been revoked");
                return;
            }

            // Token valid â€” load user from DB
            String      email       = jwtUtils.getEmailFromToken(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("Authenticated user '{}' for path: {}", email, path);

        } catch (UsernameNotFoundException e) {
            // Token was valid but the user has been deleted since it was issued
            log.warn("Token valid but user not found: {}", e.getMessage());
            writeUnauthorized(response, "User account no longer exists");
            return;

        } catch (Exception e) {
            // Catch-all â€” never expose stack traces to the client
            log.error("Unexpected error in JWT filter: {}", e.getMessage(), e);
            writeError(response, 500, "Internal Server Error",
                       "Authentication processing failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        // Standard: "Authorization: Bearer <token>"
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }

        // Some clients send: "Authorization: <token>" without "Bearer" prefix
        // Uncomment below if you need to support that:
        // if (StringUtils.hasText(header) && !header.contains(" ")) return header.trim();

        return null;
    }

    private void writeUnauthorized(HttpServletResponse response,
                                   String message) throws IOException {
        writeError(response, 401, "Unauthorized", message);
    }

    private void writeError(HttpServletResponse response,
                            int status, String error,
                            String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format(
            "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
            status, error, message);

        PrintWriter writer = response.getWriter();
        writer.print(json);
        writer.flush();
    }
}
