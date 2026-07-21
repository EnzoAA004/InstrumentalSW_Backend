package com.instrumentalsw.backend.application;

import com.instrumentalsw.backend.domain.TranscriptionJob;

public interface TranscriptionGateway {
    TranscriptionJob submit(TranscriptionUpload upload);
}
