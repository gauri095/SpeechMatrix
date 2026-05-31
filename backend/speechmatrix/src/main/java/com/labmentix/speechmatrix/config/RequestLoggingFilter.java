package com.labmentix.speechmatrix.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs every inbound HTTP request and its response.
 *
 * Log format (one line per request):
 *   → GET  /api/speech/history  [requestId=abc123] user=arjun@test.com
 *   ← 200  GET  /api/speech/history  45ms  [requestId=abc123]
 *
 * The requestId is placed in MDC (Mapped Diagnostic Context) so it
 * appears in every log line emitted during that request's lifecycle —
 * making it trivial to grep all logs for a single request.
 *
 * Sensitive paths (/api/auth/register, /api/auth/login) have their
 * request body suppressed — never log passwords.
 */
@Component
@Order(1)          // run before Spring Security
@Slf4j
public class RequestLoggingFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID    = "requestId";

    // Paths where we log extra detail
    private static final String[] VERBOSE_PATHS = {
        "/api/speech/", "/api/dev/"
    };

    // Paths where request body is NEVER logged (contains passwords)
    private static final String[] SENSITIVE_PATHS = {
        "/api/auth/register", "/api/auth/login", "/api/auth/change-password"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        // ── Assign request ID ─────────────────────────────────────────────────
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);  // echo back in response

        // ── Wrap for body caching ─────────────────────────────────────────────
        ContentCachingRequestWrapper  wrappedReq =
            new ContentCachingRequestWrapper(request, 1024);
        ContentCachingResponseWrapper wrappedRes =
            new ContentCachingResponseWrapper(response);

        String method = request.getMethod();
        String path   = request.getRequestURI();
        String query  = request.getQueryString();
        String full   = query != null ? path + "?" + query : path;

        long start = System.currentTimeMillis();

        // ── Log incoming request ──────────────────────────────────────────────
        if (isVerbose(path)) {
            String user = extractUser(request);
            log.info("→ {} {} | user={} | id={}",
                method, full, user, requestId);
        } else {
            log.debug("→ {} {}", method, full);
        }

        try {
            chain.doFilter(wrappedReq, wrappedRes);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int  status  = wrappedRes.getStatus();

            // ── Log response ──────────────────────────────────────────────────
            if (status >= 400) {
                // Always log errors at WARN/ERROR level
                if (status >= 500) {
                    log.error("← {} {} {} {}ms | id={}",
                        status, method, path, elapsed, requestId);
                } else {
                    log.warn("← {} {} {} {}ms | id={}",
                        status, method, path, elapsed, requestId);
                }
            } else if (isVerbose(path)) {
                log.info("← {} {} {} {}ms | id={}",
                    status, method, path, elapsed, requestId);
            } else {
                log.debug("← {} {} {} {}ms", status, method, path, elapsed);
            }

            // ── Slow request warning ──────────────────────────────────────────
            if (elapsed > 5000) {
                log.warn("SLOW REQUEST: {} {} took {}ms", method, path, elapsed);
            }

            // ── Copy cached body back to real response ────────────────────────
            wrappedRes.copyBodyToResponse();

            MDC.remove(MDC_REQUEST_ID);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isVerbose(String path) {
        for (String p : VERBOSE_PATHS) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    private boolean isSensitive(String path) {
        for (String p : SENSITIVE_PATHS) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    private String extractUser(HttpServletRequest request) {
        // Try X-User header (set downstream by auth filter)
        String user = request.getHeader("X-User");
        if (user != null) return user;

        // Try to get email from JWT if already authenticated
        java.security.Principal principal = request.getUserPrincipal();
        if (principal != null) return principal.getName();

        return "anonymous";
    }
}