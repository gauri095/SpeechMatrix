package com.labmentix.service;

import com.labmentix.speechmatrix.dto.AuthDto;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.repository.UserRepository;
import com.labmentix.speechmatrix.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests")
class UserServiceTest {

    @Mock UserRepository           userRepository;
    @Mock TranscriptionRepository  transcriptionRepository;
    @Mock PasswordEncoder          passwordEncoder;
    @Mock AuthenticationManager    authenticationManager;
    @Mock JwtUtils                 jwtUtils;

    @InjectMocks UserService userService;

    private AuthDto.RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDto.RegisterRequest();
        registerRequest.setName("Priya Nair");
        registerRequest.setEmail("priya@sttapp.com");
        registerRequest.setPassword("secret123");
    }

    
    @Test
    @DisplayName("register saves user and returns token")
    void registerSuccess() {
        when(userRepository.existsByEmail("priya@sttapp.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hashed");
        when(jwtUtils.generateTokenFromEmail("priya@sttapp.com")).thenReturn("mock.jwt.token");

        User savedUser = User.builder()
                .id(1L).name("Priya Nair").email("priya@sttapp.com")
                .password("$2a$hashed").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthDto.AuthResponse response = userService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getEmail()).isEqualTo("priya@sttapp.com");
        assertThat(response.getName()).isEqualTo("Priya Nair");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    @DisplayName("register throws when email already taken")
    void registerDuplicateEmail() {
        when(userRepository.existsByEmail("priya@sttapp.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

   
    @Test
    @DisplayName("login returns token for correct credentials")
    void loginSuccess() {
        AuthDto.LoginRequest loginRequest = new AuthDto.LoginRequest();
        loginRequest.setEmail("priya@sttapp.com");
        loginRequest.setPassword("secret123");

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken("priya@sttapp.com", "secret123");
        when(authenticationManager.authenticate(any())).thenReturn(authToken);

        User user = User.builder().id(1L).name("Priya Nair")
                .email("priya@sttapp.com").password("hashed").build();
        when(userRepository.findByEmail("priya@sttapp.com"))
                .thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any())).thenReturn("login.jwt.token");

        AuthDto.AuthResponse response = userService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("login.jwt.token");
        assertThat(response.getId()).isEqualTo(1L);
    }
}
