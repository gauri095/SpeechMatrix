package com.labmentix.speechmatrix.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter unit tests")
class JwtAuthFilterTest {

    @Mock JwtUtils                jwtUtils;
    @Mock UserDetailsServiceImpl  userDetailsService;
    @Mock HttpServletRequest      request;
    @Mock HttpServletResponse     response;
    @Mock FilterChain             filterChain;

    @InjectMocks JwtAuthFilter filter;

    StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();
        responseBody = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
        lenient().when(request.getRequestURI()).thenReturn("/api/speech/history");
        lenient().when(request.getMethod()).thenReturn("GET");
    }

    // â??â?? No token â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("No Authorization header â?? chain continues, no auth set")
    void noAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Authorization header with no value after Bearer â?? treated as no token")
    void emptyBearerToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // â??â?? Invalid token â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("Invalid token â?? returns 401 JSON, chain does NOT continue")
    void invalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token.here");
        when(jwtUtils.validateToken("bad.token.here")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        String body = responseBody.toString();
        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("invalid");
    }

    // â??â?? Valid token â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("Valid token â?? authentication set in SecurityContext")
    void validToken() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getEmailFromToken(token)).thenReturn("arjun@sttapp.com");

        UserDetails userDetails = new User(
            "arjun@sttapp.com", "hashed", Collections.emptyList());
        when(userDetailsService.loadUserByUsername("arjun@sttapp.com"))
            .thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
            .isNotNull();
        assertThat(SecurityContextHolder.getContext()
            .getAuthentication().getName())
            .isEqualTo("arjun@sttapp.com");
    }

    // â??â?? User deleted â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("Valid token but user deleted â?? returns 401, chain stops")
    void validTokenUserDeleted() throws Exception {
        String token = "valid.but.deleted.user";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getEmailFromToken(token)).thenReturn("deleted@sttapp.com");
        when(userDetailsService.loadUserByUsername("deleted@sttapp.com"))
            .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());

        String body = responseBody.toString();
        assertThat(body).contains("no longer exists");
    }

    // â??â?? Public path skipping â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("shouldNotFilter returns true for /api/auth/login")
    void publicPathSkipped() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter returns true for /api/auth/register")
    void registerPathSkipped() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter returns false for /api/speech/history")
    void protectedPathNotSkipped() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/speech/history");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }
}
