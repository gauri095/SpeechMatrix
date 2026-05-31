package com.labmentix.speechmatrix.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthErrorResponse utility tests")
class AuthErrorResponseTest {

    @Mock HttpServletResponse response;
    @Mock HttpServletRequest  request;

    @Test
    @DisplayName("write() sets correct status and content-type")
    void writeSetsStatusAndContentType() throws Exception {
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        when(request.getRequestURI()).thenReturn("/api/speech/history");

        AuthErrorResponse.write(response, request, AuthErrorResponse.INVALID_TOKEN);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    @DisplayName("write() outputs valid JSON with all required fields")
    void writeOutputsValidJson() throws Exception {
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        when(request.getRequestURI()).thenReturn("/api/speech/history");

        AuthErrorResponse.write(response, request, AuthErrorResponse.INVALID_TOKEN);

        String json = sw.toString();
        assertThat(json)
            .contains("\"status\":401")
            .contains("\"error\":\"Unauthorized\"")
            .contains("\"message\":")
            .contains("\"path\":\"/api/speech/history\"")
            .contains("\"timestamp\":");
    }

    @Test
    @DisplayName("MISSING_TOKEN shape has status 401")
    void missingTokenShape() {
        assertThat(AuthErrorResponse.MISSING_TOKEN.getStatus()).isEqualTo(401);
        assertThat(AuthErrorResponse.MISSING_TOKEN.getError()).isEqualTo("Unauthorized");
        assertThat(AuthErrorResponse.MISSING_TOKEN.getHint()).isNotBlank();
    }

    @Test
    @DisplayName("ACCESS_DENIED shape has status 403")
    void accessDeniedShape() {
        assertThat(AuthErrorResponse.ACCESS_DENIED.getStatus()).isEqualTo(403);
        assertThat(AuthErrorResponse.ACCESS_DENIED.getError()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("write() with null hint omits hint field from JSON")
    void writeOmitsNullHint() throws Exception {
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        when(request.getRequestURI()).thenReturn("/api/speech/history");

        // ACCESS_DENIED has null hint
        AuthErrorResponse.write(response, request, AuthErrorResponse.ACCESS_DENIED);

        assertThat(sw.toString()).doesNotContain("\"hint\"");
    }
}
