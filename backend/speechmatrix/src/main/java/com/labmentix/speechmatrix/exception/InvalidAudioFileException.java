package com.labmentix.speechmatrix.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidAudioFileException extends RuntimeException {
    public InvalidAudioFileException(String message) { super(message); }
}
