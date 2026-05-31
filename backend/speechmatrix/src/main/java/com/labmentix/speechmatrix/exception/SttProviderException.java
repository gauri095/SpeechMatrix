package com.labmentix.speechmatrix.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class SttProviderException extends RuntimeException {
    public SttProviderException(String message) { super(message); }
    public SttProviderException(String message, Throwable cause) { super(message, cause); }
}
