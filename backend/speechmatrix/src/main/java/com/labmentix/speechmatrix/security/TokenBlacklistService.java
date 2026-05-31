package com.labmentix.speechmatrix.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a set of invalidated JWT tokens (logged-out tokens).
 *
 * Uses an in-memory ConcurrentHashMap keyed by token â†’ expiry epoch.
 * A scheduled task purges expired entries every hour so it doesn't grow forever.
 *
 * For multi-instance deployments: replace this with a Redis SET with TTL.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    // token string â†’ expiry epoch millis
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /** Mark a token as invalid. expiryMs = when the JWT itself expires. */
    public void blacklist(String token, long expiryMs) {
        blacklist.put(token, expiryMs);
        log.debug("Token blacklisted (total blacklisted: {})", blacklist.size());
    }

    /** Returns true if this token has been invalidated by logout. */
    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /** Runs every hour â€” removes entries whose JWT has already expired anyway. */
    @Scheduled(fixedRateString = "PT1H")
    public void purgeExpired() {
        long now = Instant.now().toEpochMilli();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(e -> e.getValue() < now);
        int removed = before - blacklist.size();
        if (removed > 0) log.info("Purged {} expired tokens from blacklist", removed);
    }
}
