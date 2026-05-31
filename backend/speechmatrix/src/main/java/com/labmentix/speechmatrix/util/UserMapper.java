package com.labmentix.speechmatrix.util;

import com.labmentix.speechmatrix.dto.AuthDto;
import com.labmentix.speechmatrix.model.User;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts User entities to DTOs / response maps.
 * Ensures the password hash is NEVER included in any response.
 */
@Component
public class UserMapper {

    /** Builds the profile response map (used by GET /api/auth/profile). */
    public Map<String, Object> toProfileMap(User user,
                                            long   totalTranscriptions,
                                            long   totalWords,
                                            double totalDurationSecs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",                  user.getId());
        map.put("name",                user.getName());
        map.put("email",               user.getEmail());
        map.put("createdAt",           user.getCreatedAt());
        map.put("totalTranscriptions", totalTranscriptions);
        map.put("totalWords",          totalWords);
        map.put("totalDurationSecs",   totalDurationSecs);
        // NOTE: password is intentionally omitted
        return map;
    }

    /** Builds the AuthResponse after register or login. */
    public AuthDto.AuthResponse toAuthResponse(User user, String token) {
        return new AuthDto.AuthResponse(
            token, user.getId(), user.getName(), user.getEmail());
    }
}
