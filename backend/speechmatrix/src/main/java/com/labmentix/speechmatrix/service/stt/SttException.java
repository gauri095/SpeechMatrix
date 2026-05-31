package com.labmentix.speechmatrix.service.stt;

/**
 * Thrown when the STT provider returns an error, times out,
 * or returns an unexpected response format.
 *
 * Caught by GlobalExceptionHandler and mapped to HTTP 502 Bad Gateway.
 */
public class SttException extends RuntimeException {

    private final int httpStatus;

    public SttException(String message) {
        super(message);
        this.httpStatus = 502;
    }

    public SttException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 502;
    }

    public SttException(String message, int providerHttpStatus) {
        super(message);
        this.httpStatus = providerHttpStatus;
    }

    public int getHttpStatus() { return httpStatus; }
}
