package com.labmentix.speechmatrix.controller;

import com.labmentix.speechmatrix.security.BCryptPasswordService;
import com.labmentix.speechmatrix.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Developer diagnostics â?? ONLY active when Spring profile is NOT "prod".
 * Never expose these endpoints in production.
 *
 * Available routes:
 *   GET  /api/dev/bcrypt?password=YourPass1         â?? hash analysis
 *   POST /api/dev/bcrypt/verify                     â?? verify plain vs hash
 *   GET  /api/dev/token/inspect                     â?? decode current JWT
 *   GET  /api/dev/security/routes                   â?? list public vs secured routes
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("!prod")   // disabled in production profile
public class DevDiagnosticController {

    private final BCryptPasswordService bcryptService;
    private final JwtUtils              jwtUtils;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // â??â?? BCrypt: hash & analyze â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @GetMapping("/bcrypt")
    public ResponseEntity<Map<String, Object>> bcryptInfo(
            @RequestParam String password) {

        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "?password= parameter is required"));
        }
        return ResponseEntity.ok(bcryptService.analyzePassword(password));
    }

    // â??â?? BCrypt: verify plain vs stored hash â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @PostMapping("/bcrypt/verify")
    public ResponseEntity<Map<String, Object>> bcryptVerify(
            @RequestBody Map<String, String> body) {

        String plain = body.get("password");
        String hash  = body.get("hash");

        if (plain == null || hash == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Both 'password' and 'hash' fields are required"));
        }

        boolean matches = bcryptService.verify(plain, hash);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matches",       matches);
        result.put("hashLength",    hash.length());
        result.put("hashPrefix",    hash.length() >= 7 ? hash.substring(0, 7) : hash);
        result.put("needsUpgrade",  bcryptService.needsUpgrade(hash));
        return ResponseEntity.ok(result);
    }

    // â??â?? Token: decode current request's JWT â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @GetMapping("/token/inspect")
    public ResponseEntity<Map<String, Object>> inspectToken(
            @AuthenticationPrincipal UserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest request) {

        String header = request.getHeader("Authorization");
        String token  = (header != null && header.startsWith("Bearer "))
                ? header.substring(7) : null;

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("email",         userDetails.getUsername());
        info.put("authorities",   userDetails.getAuthorities());
        info.put("accountNonExpired",   userDetails.isAccountNonExpired());
        info.put("accountNonLocked",    userDetails.isAccountNonLocked());
        info.put("credentialsNonExpired",userDetails.isCredentialsNonExpired());

        if (token != null) {
            try {
                long expiryMs = jwtUtils.getExpiryMs(token);
                LocalDateTime expiresAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(expiryMs), ZoneId.systemDefault());
                long remainingMs = expiryMs - System.currentTimeMillis();

                info.put("tokenValid",      true);
                info.put("expiresAt",       expiresAt.toString());
                info.put("remainingMs",     remainingMs);
                info.put("remainingMins",   remainingMs / 60_000);
                info.put("configuredTtlMs", jwtExpirationMs);
                info.put("tokenLength",     token.length());
            } catch (Exception e) {
                info.put("tokenParseError", e.getMessage());
            }
        }
        return ResponseEntity.ok(info);
    }

    // â”€â”€ Security: list all route rules â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/security/routes")
    public ResponseEntity<Map<String, Object>> securityRoutes() {
        Map<String, Object> routes = new LinkedHashMap<>();

        routes.put("publicRoutes", new String[]{
            "POST /api/auth/register",
            "POST /api/auth/login",
            "GET  /actuator/health",
            "WS   /ws/**"
        });
        routes.put("protectedRoutes", new String[]{
            "POST /api/auth/logout",
            "GET  /api/auth/me",
            "GET  /api/auth/profile",
            "PUT  /api/auth/profile",
            "POST /api/auth/change-password",
            "POST /api/speech/upload",
            "GET  /api/speech/history",
            "GET  /api/speech/:id",
            "GET  /api/speech/:id/export"
        });
        routes.put("devOnlyRoutes", new String[]{
            "GET  /api/dev/bcrypt",
            "POST /api/dev/bcrypt/verify",
            "GET  /api/dev/token/inspect",
            "GET  /api/dev/security/routes"
        });
        routes.put("activeProfile", "dev (use --spring.profiles.active=prod to disable)");

        return ResponseEntity.ok(routes);
    }
}
