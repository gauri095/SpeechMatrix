package com.labmentix.speechmatrix.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return createToken(userDetails.getUsername());
    }

    public String generateTokenFromEmail(String email) {
        return createToken(email);
    }

    public String getEmailFromToken(String token) {
        String payloadJson = getPayloadFromToken(token);
        String subject = extractClaim(payloadJson, "sub");
        if (subject == null || subject.isEmpty()) {
            throw new IllegalArgumentException("JWT token does not contain subject");
        }
        return subject;
    }

    public boolean validateToken(String token) {
        try {
            getPayloadFromToken(token);
            return true;
        } catch (IllegalArgumentException | SecurityException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    private String createToken(String subject) {
        long now = System.currentTimeMillis();
        long expiration = now + jwtExpirationMs;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
                escapeJson(subject), now / 1000, expiration / 1000);

        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = base64UrlEncode(hmacSha256((header + "." + payload).getBytes(StandardCharsets.UTF_8)));

        return header + "." + payload + "." + signature;
    }

    private String getPayloadFromToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token structure");
        }

        if (!verifySignature(parts[0], parts[1], parts[2])) {
            throw new SecurityException("Invalid JWT signature");
        }

        String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
        if (isExpired(payloadJson)) {
            throw new IllegalArgumentException("JWT token expired");
        }

        return payloadJson;
    }

    private boolean verifySignature(String header, String payload, String signature) {
        String signingInput = header + "." + payload;
        String expectedSignature = base64UrlEncode(hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8)));
        return expectedSignature.equals(signature);
    }

    private boolean isExpired(String payloadJson) {
        String expValue = extractClaim(payloadJson, "exp");
        if (expValue == null) {
            return true;
        }
        try {
            long exp = Long.parseLong(expValue);
            long now = System.currentTimeMillis() / 1000;
            return now >= exp;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private String extractClaim(String json, String claimName) {
        String prefix = "\"" + claimName + "\":";
        int index = json.indexOf(prefix);
        if (index == -1) {
            return null;
        }

        int valueStart = index + prefix.length();
        if (valueStart >= json.length()) {
            return null;
        }

        char firstChar = json.charAt(valueStart);
        if (firstChar == '"') {
            int endQuote = json.indexOf('"', valueStart + 1);
            if (endQuote == -1) {
                return null;
            }
            return json.substring(valueStart + 1, endQuote);
        }

        int endIndex = valueStart;
        while (endIndex < json.length() && Character.isDigit(json.charAt(endIndex))) {
            endIndex++;
        }
        return json.substring(valueStart, endIndex);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private byte[] hmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
