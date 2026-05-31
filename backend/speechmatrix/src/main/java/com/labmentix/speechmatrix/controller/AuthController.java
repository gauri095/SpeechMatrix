package com.labmentix.speechmatrix.controller;

import com.labmentix.speechmatrix.dto.AuthDto;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.UserRepository;
import com.labmentix.speechmatrix.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Email is already registered"));
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        String token = jwtUtils.generateTokenFromEmail(user.getEmail());

        return ResponseEntity.ok(
                new AuthDto.AuthResponse(token, user.getId(),
                        user.getName(), user.getEmail()));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        String token = jwtUtils.generateToken(authentication);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        return ResponseEntity.ok(
                new AuthDto.AuthResponse(token, user.getId(),
                        user.getName(), user.getEmail()));
    }
}
