package com.labmentix.speechmatrix.service;

import com.labmentix.speechmatrix.dto.AuthDto;
import com.labmentix.speechmatrix.exception.ResourceNotFoundException;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.repository.UserRepository;
import com.labmentix.speechmatrix.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository            userRepository;
    private final TranscriptionRepository   transcriptionRepository;
    private final PasswordEncoder           passwordEncoder;
    private final AuthenticationManager     authenticationManager;
    private final JwtUtils                  jwtUtils;

    // â??â?? Register â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        log.debug("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                "Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: id={}, email={}", user.getId(), user.getEmail());

        String token = jwtUtils.generateTokenFromEmail(user.getEmail());
        return new AuthDto.AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }

    
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        log.debug("Login attempt: {}", request.getEmail());

        // Throws BadCredentialsException if wrong â?? Spring handles the 401
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtUtils.generateToken(auth);
        log.info("User logged in: {}", user.getEmail());

        return new AuthDto.AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }

   @Transactional(readOnly = true)
    public Map<String, Object> getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found: " + email));

        long   totalTranscriptions = transcriptionRepository.countByUserId(user.getId());
        Long   totalWords          = transcriptionRepository.sumWordCountByUserId(user.getId());
        Double totalDuration       = transcriptionRepository.sumDurationByUserId(user.getId());

        return Map.of(
            "id",                  user.getId(),
            "name",                user.getName(),
            "email",               user.getEmail(),
            "createdAt",           user.getCreatedAt(),
            "totalTranscriptions", totalTranscriptions,
            "totalWords",          totalWords   != null ? totalWords   : 0L,
            "totalDurationSecs",   totalDuration != null ? totalDuration : 0.0
        );
    }

  @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found: " + email));
    }
}
