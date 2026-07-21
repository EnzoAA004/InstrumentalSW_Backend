package com.instrumentalsw.backend.web;

import com.instrumentalsw.backend.domain.TranscriptionException;
import com.instrumentalsw.backend.domain.UploadErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public final class ApiExceptionHandler {
    @ExceptionHandler(TranscriptionException.class)
    ResponseEntity<ApiErrorResponse> handleControlled(TranscriptionException error) {
        return ResponseEntity.status(statusFor(error.code()))
                .body(new ApiErrorResponse(error.code().name(), error.getMessage(), error.field()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> handleSizeLimit(MaxUploadSizeExceededException error) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiErrorResponse(
                        UploadErrorCode.AUDIO_SIZE_LIMIT_EXCEEDED.name(),
                        "The audio exceeds the accepted transport size limit.",
                        "file"));
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ApiErrorResponse> handleMultipart(MultipartException error) {
        return ResponseEntity.unprocessableEntity()
                .body(new ApiErrorResponse(
                        UploadErrorCode.INVALID_TRANSCRIPTION_REQUEST.name(),
                        "The multipart transcription request is invalid.",
                        null));
    }

    private static HttpStatus statusFor(UploadErrorCode code) {
        return switch (code) {
            case AUDIO_FILE_REQUIRED, EMPTY_AUDIO_FILE, INVALID_SAXOPHONE_TYPE, INVALID_INPUT_MODE -> HttpStatus
                    .BAD_REQUEST;
            case UNSUPPORTED_AUDIO_FORMAT -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case AUDIO_SIZE_LIMIT_EXCEEDED -> HttpStatus.PAYLOAD_TOO_LARGE;
            case INVALID_TRANSCRIPTION_REQUEST -> HttpStatus.UNPROCESSABLE_ENTITY;
            case AI_SERVICE_UNAVAILABLE, AI_SERVICE_ERROR -> HttpStatus.BAD_GATEWAY;
        };
    }
}
