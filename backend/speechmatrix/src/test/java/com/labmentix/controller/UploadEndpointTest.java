package com.labmentix.speechmatrix.controller;

import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.exception.InvalidAudioFileException;
import com.labmentix.speechmatrix.service.TranscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpeechController.class)
@DisplayName("SpeechController upload integration tests")
class UploadEndpointTest {

    @Autowired MockMvc mockMvc;
    @MockBean  TranscriptionService transcriptionService;

    private static final byte[] MP3_MAGIC =
        { 0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

    // â”€â”€ POST /upload â€” happy path â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("POST /upload returns 201 with transcript on success")
    @WithMockUser(username = "arjun@test.com")
    void uploadSuccess() throws Exception {
        TranscriptionDto.Response resp = new TranscriptionDto.Response();
        resp.setId(1L);
        resp.setTranscript("Hello from the audio file");
        resp.setWordCount(5);
        resp.setStatus("done");

        when(transcriptionService.uploadAndTranscribe(
            eq("arjun@test.com"), any(), any())).thenReturn(resp);

        MockMultipartFile file = new MockMultipartFile(
            "file", "interview.mp3", "audio/mpeg", MP3_MAGIC);

        mockMvc.perform(multipart("/api/speech/upload")
                .file(file)
                .param("language", "en-US"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.transcript").value("Hello from the audio file"))
            .andExpect(jsonPath("$.data.wordCount").value(5))
            .andExpect(jsonPath("$.message")
                .value("Audio uploaded and transcribed successfully"));
    }

    @Test
    @DisplayName("POST /upload without language param defaults to en-US")
    @WithMockUser(username = "arjun@test.com")
    void uploadDefaultsLanguage() throws Exception {
        when(transcriptionService.uploadAndTranscribe(any(), any(), any()))
            .thenReturn(new TranscriptionDto.Response());

        MockMultipartFile file = new MockMultipartFile(
            "file", "audio.mp3", "audio/mpeg", MP3_MAGIC);

        mockMvc.perform(multipart("/api/speech/upload").file(file))
            .andExpect(status().isCreated());

        verify(transcriptionService).uploadAndTranscribe(
            eq("arjun@test.com"),
            any(),
            argThat(req -> "en-US".equals(req.getLanguage()))
        );
    }

    @Test
    @DisplayName("POST /upload with speakerCount param passes it through")
    @WithMockUser(username = "arjun@test.com")
    void uploadWithSpeakerCount() throws Exception {
        when(transcriptionService.uploadAndTranscribe(any(), any(), any()))
            .thenReturn(new TranscriptionDto.Response());

        MockMultipartFile file = new MockMultipartFile(
            "file", "meeting.mp3", "audio/mpeg", MP3_MAGIC);

        mockMvc.perform(multipart("/api/speech/upload")
                .file(file)
                .param("language", "en-US")
                .param("speakerCount", "3"))
            .andExpect(status().isCreated());

        verify(transcriptionService).uploadAndTranscribe(
            any(),
            any(),
            argThat(req -> Integer.valueOf(3).equals(req.getSpeakerCount()))
        );
    }

    // â”€â”€ POST /upload â€” error cases â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("POST /upload without file returns 400")
    @WithMockUser(username = "arjun@test.com")
    void uploadMissingFileReturns400() throws Exception {
        mockMvc.perform(multipart("/api/speech/upload")
                .param("language", "en-US"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /upload when validator throws returns 422")
    @WithMockUser(username = "arjun@test.com")
    void uploadInvalidFileReturns422() throws Exception {
        when(transcriptionService.uploadAndTranscribe(any(), any(), any()))
            .thenThrow(new InvalidAudioFileException("Unsupported file format: .exe"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "virus.exe", "application/octet-stream",
            new byte[]{ 0x4D, 0x5A });

        mockMvc.perform(multipart("/api/speech/upload").file(file))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
            .andExpect(jsonPath("$.message").value("Unsupported file format: .exe"));
    }

    @Test
    @DisplayName("POST /upload without auth returns 401")
    void uploadNoAuthReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "audio.mp3", "audio/mpeg", MP3_MAGIC);

        mockMvc.perform(multipart("/api/speech/upload").file(file))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /upload consumes multipart/form-data")
    @WithMockUser(username = "arjun@test.com")
    void uploadRequiresMultipart() throws Exception {
        // Sending JSON body to multipart endpoint should fail
        mockMvc.perform(post("/api/speech/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"file\": \"fake\"}"))
            .andExpect(status().isUnsupportedMediaType());
    }
}
