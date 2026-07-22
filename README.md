# InstrumentalSW Backend

Spring Boot product API for InstrumentalSW (Saxo). SAX-040 exposes the browser-facing upload gateway, SAX-041 adds read-only job status retrieval, and SAX-042 adds validated read-only note review retrieval through the existing internal FastAPI AI service.

```text
Next.js :3000
  → Spring Boot :8080
    → FastAPI :8000
```

The browser never calls FastAPI directly. Audio bytes are held only for the upload request and are not persisted. Job status and review snapshots are not stored or reconstructed in Spring.

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

The multipart values are transport limits. FastAPI remains authoritative for AI-service functional limits and job/review data.

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

Spring forwards the exact UUID to FastAPI, accepts only HTTP 200, and preserves the seven job fields. Malformed UUIDs return `400 INVALID_JOB_ID`; unknown jobs return `404 TRANSCRIPTION_NOT_FOUND`; unavailable or incompatible upstream responses become controlled 502 errors. The current AI statuses are `UPLOADED` and `FAILED`; Spring adds no transitions.

## Get transcription notes for review

```http
GET /api/v1/transcriptions/{job_id}/review
Accept: application/json
```

Example:

```bash
curl http://localhost:8080/api/v1/transcriptions/11111111-1111-1111-1111-111111111111/review
```

Spring sends one bodyless GET to FastAPI and validates the complete versioned response: UUID identity, policy/schema versions, instrument, threshold, confidence interpretation/method, counts, ordered event indices, concert/written MIDI, timing, velocity, confidence, and low-confidence markers.

A known job without a registered result returns:

```json
{
  "code": "TRANSCRIPTION_RESULT_NOT_READY",
  "message": "Transcription notes are not available yet.",
  "field": "job_id"
}
```

with HTTP 409. An empty successful review is distinct and returns HTTP 200 with zero events. The Backend does not run inference, create notes, derive duration fields, or persist the payload.

## CORS

Only `SAXO_FRONTEND_ORIGIN` may use POST, GET, and OPTIONS on the transcription API. Credentials are disabled and wildcard origins are not used.

## Security and boundaries

- filename path components are removed before upload forwarding;
- only `.mp3` and `.wav` are accepted case-insensitively;
- MIME type is preserved when available but is not trusted as the format authority;
- audio, multipart bodies, internal URLs, upstream HTML, stack traces, and paths are not exposed publicly;
- the Backend does not calculate a replacement SHA-256;
- every status/review GET reaches FastAPI once and uses no local repository;
- review payloads are request-scoped and not persisted;
- there is no inference, automatic upload processing, audio storage, BackgroundTasks, database, queue, worker, new job status, review polling, WebSocket, SSE, editing, playback, SAX-043, or later story in SAX-042.

Contracts and evidence:

- [`docs/contracts/audio-upload-gateway-v1.md`](docs/contracts/audio-upload-gateway-v1.md)
- [`docs/contracts/job-status-gateway-v1.md`](docs/contracts/job-status-gateway-v1.md)
- [`docs/contracts/transcription-review-gateway-v1.md`](docs/contracts/transcription-review-gateway-v1.md)
- [`docs/tdd/iteration-001.md`](docs/tdd/iteration-001.md)
- [`docs/tdd/iteration-002.md`](docs/tdd/iteration-002.md)
- [`docs/tdd/iteration-003.md`](docs/tdd/iteration-003.md)
