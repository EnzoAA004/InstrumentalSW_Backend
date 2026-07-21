package com.instrumentalsw.backend.domain;

public final class TranscriptionException extends RuntimeException {
    private final UploadErrorCode code;
    private final String field;

    public TranscriptionException(UploadErrorCode code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public TranscriptionException(UploadErrorCode code, String message, String field, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.field = field;
    }

    public UploadErrorCode code() {
        return code;
    }

    public String field() {
        return field;
    }
}
