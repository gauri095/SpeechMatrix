package com.labmentix.service;

import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.exception.SttProviderException;
import com.labmentix.speechmatrix.model.Transcription;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.service.stt.SttException;
import com.labmentix.speechmatrix.service.stt.SttResult;
import com.labmentix.speechmatrix.util.AudioFileValidator;
import com.labmentix.speechmatrix.util.TranscriptionMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptionService — upload pipeline tests")
class TranscriptionServiceUploadTest {

    @Mock TranscriptionRepository transcriptionRepository;
    @Mock TranscriptionMapper     transcriptionMapper;
    @Mock UserService             userService;
    @Mock AudioStorageService     audioStorageService;
    @Mock AudioFileValidator      audioFileValidator;
    @Mock SpeechService           speechService;

    @InjectMocks TranscriptionService service;

    @TempDir Path tempDir;

    User testUser;
    MockMultipartFile mp3File;

    static final byte[] MP3_BYTES = {
        0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x54, 0x45, 0x53, 0x54
    };

    @BeforeEach
    void setUp() throws Exception {
        testUser = User.builder().id(1L).name("Arjun")
                .email("arjun@test.com").password("hashed").build();

        mp3File = new MockMultipartFile(
            "file", "lecture.mp3", "audio/mpeg", MP3_BYTES);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("uploadAndTranscribe — success path saves transcript and returns response")
    void uploadSuccessPath() throws Exception {
        // Arrange
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        doNothing().when(audioFileValidator).validate(any(MultipartFile.class));
        when(audioFileValidator.getMimeType(any())).thenReturn("audio/mpeg");
        when(audioStorageService.store(any(), eq(1L))).thenReturn("uploads/1/abc_lecture.mp3");
        when(speechService.getProviderName()).thenReturn("mock");

        // Saved row with id set
        Transcription saved = Transcription.builder()
            .user(testUser).audioFile("uploads/1/abc_lecture.mp3")
            .language("en-US").status("processing").build();
        saved.setId(10L);
        when(transcriptionRepository.save(any())).thenReturn(saved);

        // Audio file for STT
        File audioFile = tempDir.resolve("abc_lecture.mp3").toFile();
        Files.write(audioFile.toPath(), MP3_BYTES);
        when(audioStorageService.load("uploads/1/abc_lecture.mp3")).thenReturn(audioFile);
        when(speechService.needsChunking(audioFile)).thenReturn(false);

        SttResult sttResult = SttResult.builder()
            .transcript("Spring Boot makes development fast")
            .confidence(0.97).durationSeconds(15.0).build();
        when(speechService.transcribeFile(any(), eq("en-US"), eq("audio/mpeg"), eq(false)))
            .thenReturn(sttResult);

        TranscriptionDto.Response expectedResponse = new TranscriptionDto.Response();
        expectedResponse.setId(10L);
        when(transcriptionMapper.toResponse(any())).thenReturn(expectedResponse);

        // Act
        TranscriptionDto.UploadRequest req = new TranscriptionDto.UploadRequest();
        req.setLanguage("en-US");
        TranscriptionDto.Response result =
            service.uploadAndTranscribe("arjun@test.com", mp3File, req);

        // Assert
        assertThat(result.getId()).isEqualTo(10L);

        // Verify the final save sets status=done
        ArgumentCaptor<Transcription> captor = ArgumentCaptor.forClass(Transcription.class);
        verify(transcriptionRepository, times(2)).save(captor.capture());
        Transcription finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getStatus()).isEqualTo(Transcription.STATUS_DONE);
        assertThat(finalSave.getTranscript()).isEqualTo("Spring Boot makes development fast");
        assertThat(finalSave.getConfidenceScore()).isEqualTo(0.97);
    }

    @Test
    @DisplayName("uploadAndTranscribe — STT failure sets status=failed and throws")
    void uploadSttFailure() throws Exception {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        doNothing().when(audioFileValidator).validate(any());
        when(audioFileValidator.getMimeType(any())).thenReturn("audio/mpeg");
        when(audioStorageService.store(any(), any())).thenReturn("uploads/1/file.mp3");
        when(speechService.getProviderName()).thenReturn("mock");

        Transcription saved = Transcription.builder()
            .user(testUser).status("processing").language("en-US").build();
        saved.setId(5L);
        when(transcriptionRepository.save(any())).thenReturn(saved);

        File audioFile = tempDir.resolve("file.mp3").toFile();
        Files.write(audioFile.toPath(), MP3_BYTES);
        when(audioStorageService.load(any())).thenReturn(audioFile);
        when(speechService.needsChunking(any())).thenReturn(false);
        when(speechService.transcribeFile(any(), any(), any(), anyBoolean()))
            .thenThrow(new SttException("Deepgram 503", 503));

        TranscriptionDto.UploadRequest req = new TranscriptionDto.UploadRequest();
        req.setLanguage("en-US");

        assertThatThrownBy(() ->
            service.uploadAndTranscribe("arjun@test.com", mp3File, req))
            .isInstanceOf(SttProviderException.class);

        // Verify status was set to failed
        ArgumentCaptor<Transcription> captor = ArgumentCaptor.forClass(Transcription.class);
        verify(transcriptionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStatus())
            .isEqualTo(Transcription.STATUS_FAILED);
    }

    @Test
    @DisplayName("uploadAndTranscribe — large file routes through transcribeLargeFile")
    void uploadLargeFileChunked() throws Exception {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        doNothing().when(audioFileValidator).validate(any());
        when(audioFileValidator.getMimeType(any())).thenReturn("audio/mpeg");
        when(audioStorageService.store(any(), any())).thenReturn("uploads/1/large.mp3");
        when(speechService.getProviderName()).thenReturn("mock");

        Transcription saved = Transcription.builder()
            .user(testUser).status("processing").language("en-US").build();
        saved.setId(7L);
        when(transcriptionRepository.save(any())).thenReturn(saved);

        File largeFile = tempDir.resolve("large.mp3").toFile();
        Files.write(largeFile.toPath(), new byte[1024]);
        when(audioStorageService.load(any())).thenReturn(largeFile);
        when(speechService.needsChunking(largeFile)).thenReturn(true); // large file!

        SttResult chunkedResult = SttResult.builder()
            .transcript("Chunked result").confidence(0.91)
            .durationSeconds(120.0).build();
        when(speechService.transcribeLargeFile(any(), any(), any()))
            .thenReturn(chunkedResult);
        when(transcriptionMapper.toResponse(any())).thenReturn(new TranscriptionDto.Response());

        TranscriptionDto.UploadRequest req = new TranscriptionDto.UploadRequest();
        req.setLanguage("en-US");
        service.uploadAndTranscribe("arjun@test.com", mp3File, req);

        verify(speechService).transcribeLargeFile(largeFile, "en-US", "audio/mpeg");
        verify(speechService, never()).transcribeFile(any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("uploadAndTranscribe — diarization enabled when speakerCount > 1")
    void diarizationEnabledForMultipleSpeakers() throws Exception {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        doNothing().when(audioFileValidator).validate(any());
        when(audioFileValidator.getMimeType(any())).thenReturn("audio/mpeg");
        when(audioStorageService.store(any(), any())).thenReturn("uploads/1/mtg.mp3");
        when(speechService.getProviderName()).thenReturn("mock");

        Transcription saved = Transcription.builder()
            .user(testUser).status("processing").language("en-US")
            .speakerCount(2).build();
        saved.setId(8L);
        when(transcriptionRepository.save(any())).thenReturn(saved);

        File audioFile = tempDir.resolve("mtg.mp3").toFile();
        Files.write(audioFile.toPath(), MP3_BYTES);
        when(audioStorageService.load(any())).thenReturn(audioFile);
        when(speechService.needsChunking(any())).thenReturn(false);
        when(speechService.transcribeFile(any(), any(), any(), anyBoolean()))
            .thenReturn(SttResult.builder().transcript("Meeting notes").confidence(0.95)
                .durationSeconds(30.0).speakerCount(2).build());
        when(transcriptionMapper.toResponse(any())).thenReturn(new TranscriptionDto.Response());

        TranscriptionDto.UploadRequest req = new TranscriptionDto.UploadRequest();
        req.setLanguage("en-US");
        req.setSpeakerCount(2);
        service.uploadAndTranscribe("arjun@test.com", mp3File, req);

        // diarize=true when speakerCount > 1
        verify(speechService).transcribeFile(any(), eq("en-US"), eq("audio/mpeg"), eq(true));
    }
}