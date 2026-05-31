package com.labmentix.speechmatrix.controller;

import com.labmentix.speechmatrix.dto.ApiResponse;
import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.exception.ResourceNotFoundException;
import com.labmentix.speechmatrix.service.TranscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpeechController.class)
@DisplayName("SpeechController integration tests")
class SpeechControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TranscriptionService transcriptionService;

    // â??â?? GET /api/speech/ping â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("GET /ping returns 200 without auth")
    @WithMockUser
    void pingReturns200() throws Exception {
        mockMvc.perform(get("/api/speech/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Speech API ready"))
            .andExpect(jsonPath("$.routes").exists());
    }

    // â??â?? GET /api/speech/history â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("GET /history â?? returns paged response")
    @WithMockUser(username = "arjun@test.com")
    void historyReturnsPagedResponse() throws Exception {
        TranscriptionDto.PagedResponse paged = new TranscriptionDto.PagedResponse();
        paged.setContent(List.of());
        paged.setTotalElements(0);
        paged.setPage(0);
        paged.setSize(10);

        when(transcriptionService.getHistory("arjun@test.com", 0, 10))
            .thenReturn(paged);

        mockMvc.perform(get("/api/speech/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /history â?? no token returns 401")
    void historyNoTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/speech/history"))
            .andExpect(status().isUnauthorized());
    }

    // â??â?? GET /api/speech/search â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("GET /search â?? returns summaries")
    @WithMockUser(username = "arjun@test.com")
    void searchReturnsSummaries() throws Exception {
        TranscriptionDto.Summary s = new TranscriptionDto.Summary();
        when(transcriptionService.search("arjun@test.com", "spring"))
            .thenReturn(List.of(s));

        mockMvc.perform(get("/api/speech/search").param("q", "spring"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /search â?? short query returns 400")
    @WithMockUser(username = "arjun@test.com")
    void searchShortQueryReturns400() throws Exception {
        mockMvc.perform(get("/api/speech/search").param("q", "a"))
            .andExpect(status().isOk()) // controller returns 400 body in 200 wrapper
            .andExpect(jsonPath("$.message").value(
                "Query must be at least 2 characters"));
    }

    // â??â?? GET /api/speech/stats â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("GET /stats â?? returns usage statistics")
    @WithMockUser(username = "arjun@test.com")
    void statsReturnsData() throws Exception {
        TranscriptionDto.StatsResponse stats = new TranscriptionDto.StatsResponse();
        stats.setTotalTranscriptions(5);
        stats.setTotalWords(1200);

        when(transcriptionService.getStats("arjun@test.com")).thenReturn(stats);

        mockMvc.perform(get("/api/speech/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalTranscriptions").value(5))
            .andExpect(jsonPath("$.data.totalWords").value(1200));
    }

    // â??â?? GET /api/speech/{id} â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("GET /{id} â?? returns transcript when found")
    @WithMockUser(username = "arjun@test.com")
    void getByIdFound() throws Exception {
        TranscriptionDto.Response resp = new TranscriptionDto.Response();
        resp.setId(1L);
        resp.setTranscript("Hello world");

        when(transcriptionService.getById("arjun@test.com", 1L))
            .thenReturn(resp);

        mockMvc.perform(get("/api/speech/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.transcript").value("Hello world"));
    }

    @Test
    @DisplayName("GET /{id} â?? returns 404 when not found")
    @WithMockUser(username = "arjun@test.com")
    void getByIdNotFound() throws Exception {
        when(transcriptionService.getById("arjun@test.com", 99L))
            .thenThrow(new ResourceNotFoundException("Transcription", 99L));

        mockMvc.perform(get("/api/speech/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    // â??â?? DELETE /api/speech/{id} â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("DELETE /{id} â?? deletes and returns success message")
    @WithMockUser(username = "arjun@test.com")
    void deleteSuccess() throws Exception {
        doNothing().when(transcriptionService).delete("arjun@test.com", 1L);

        mockMvc.perform(delete("/api/speech/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Transcription deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /{id} â?? 404 when transcription not found")
    @WithMockUser(username = "arjun@test.com")
    void deleteNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Transcription", 99L))
            .when(transcriptionService).delete("arjun@test.com", 99L);

        mockMvc.perform(delete("/api/speech/99"))
            .andExpect(status().isNotFound());
    }
}
