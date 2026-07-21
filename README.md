# InstrumentalSW Backend

Spring Boot product API for InstrumentalSW (Saxo). SAX-040 exposes the browser-facing upload gateway; SAX-041 adds read-only job status retrieval through the existing internal FastAPI AI service.

```text
Next.js :3000
  → Spring Boot :8080
    → FastAPI :8000
```

The browser never calls FastAPI directly. Audio bytes are held only for the upload request and are not persisted. Job status is not stored or reconstructed in Spring.

## Requirements

- Java 21
- Maven Wrapper included in the repository

Pinned build stack:

```text
Spring Boot:    3.5.16
Maven Wrapper:  3.3.4
Maven:          3.9.16
JaCoCo:         0.8.13
Spotless:       2.44.5
Checkstyle:     10.21.4
```

## Verify

```bash
./mvnw verify
```

The command compiles for Java 21 and runs unit tests, MVC tests, real HTTP integration tests, Spotless, Checkstyle, and a JaCoCo line-coverage gate of at least 90%.

On Windows:

```powershell
mvnw.cmd verify
```

## Run

Start the existing AI service on port 8000, then run:

```bash
./mvnw spring-boot:run
```

The product API listens on `http://localhost:8080`.

## Environment variables

| Variable | Local default | Purpose |
| --- | --- | --- |
| `SAXO_AI_BASE_URL` | `http://localhost:8000` | Internal FastAPI base URL |
| `SAXO_AI_CONNECT_TIMEOUT` | `2s` | Connection timeout |
| `SAXO_AI_READ_TIMEOUT` | `30s` | Upstream response timeout |
| `SAXO_FRONTEND_ORIGIN` | `http://localhost:3000` | Allowed browser origin |
| `SAXO_MAX_MULTIPART_FILE_SIZE` | `101MB` | Product transport file barrier |
| `SAXO_MAX_MULTIPART_REQUEST_SIZE` | `102MB` | Product transport request barrier |

The multipart values are transport limits. FastAPI remains authoritative for AI-service functional limits and job data.

## Create a transcription job

```http
POST /api/v1/transcriptions
Content-Type: multipart/form-data
```

Exact fields and values:

```text
file
saxophone_type: soprano | alto | tenor | baritone
input_mode:     solo | mixture
```

Example using a synthetic or otherwise legally usable WAV:

```bash
curl --request POST http://localhost:8080/api/v1/transcriptions \
  --form 'file=@synthetic.wav;type=audio/wav' \
  --form 'saxophone_type=alto' \
  --form 'input_mode=solo'
```

Successful creation returns HTTP 202.

## Get current job status

```http
GET /api/v1/transcriptions/{job_id}
Accept: application/json
```

Example:

```bash
curl http://localhost:8080/api/v1/transcriptions/11111111-1111-1111-1111-111111111111
```

Spring forwards the exact UUID to FastAPI, accepts only HTTP 200, and preserves exactly:

```json
{
  "job_id": "11111111-1111-1111-1111-111111111111",
  "status": "UPLOADED",
  "filename": "synthetic.wav",
  "size_bytes": 12345,
  "audio_sha256": "64 lowercase hexadecimal characters",
  "saxophone_type": "alto",
  "input_mode": "solo"
}
```

Malformed UUIDs return `400 INVALID_JOB_ID`; unknown jobs return `404 TRANSCRIPTION_NOT_FOUND`; unavailable or incompatible upstream responses become controlled 502 errors. The current AI statuses are `UPLOADED` and `FAILED`; Spring does not add transitions.

## CORS

Only `SAXO_FRONTEND_ORIGIN` may use POST, GET, and OPTIONS on the transcription API. Credentials are disabled and wildcard origins are not used.

## Security and boundaries

- filename path components are removed before upload forwarding;
- only `.mp3` and `.wav` are accepted case-insensitively;
- MIME type is preserved when available but is not trusted as the format authority;
- audio, multipart bodies, internal URLs, upstream HTML, stack traces, and paths are not exposed publicly;
- the Backend does not calculate a replacement SHA-256;
- every status GET reaches FastAPI once and uses no local repository;
- there is no authentication, persistence, database, queue, worker, automatic retry, polling, WebSocket, or SSE in SAX-041.

Contracts and evidence:

- [`docs/contracts/audio-upload-gateway-v1.md`](docs/contracts/audio-upload-gateway-v1.md)
- [`docs/contracts/job-status-gateway-v1.md`](docs/contracts/job-status-gateway-v1.md)
- [`docs/tdd/iteration-001.md`](docs/tdd/iteration-001.md)
- [`docs/tdd/iteration-002.md`](docs/tdd/iteration-002.md)
