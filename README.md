# InstrumentalSW Backend

Spring Boot product API for InstrumentalSW (Saxo). SAX-040 exposes browser upload, SAX-041 job status, SAX-042 read-only note review, and SAX-043 immutable revision editing through the existing FastAPI AI service.

```text
Next.js :3000
  → Spring Boot :8080
    → FastAPI :8000
```

The browser never calls FastAPI directly. Spring does not store audio, jobs, reviews, revisions, or regeneration requests.

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

The multipart values are transport limits. FastAPI remains authoritative for functional limits, job state, review results, revision history, and regeneration requests.

## Create a transcription job

```http
POST /api/v1/transcriptions
Content-Type: multipart/form-data
```

Exact fields:

```text
file
saxophone_type: soprano | alto | tenor | baritone
input_mode:     solo | mixture
```

Successful creation returns HTTP 202. Spring streams the request to FastAPI and persists no audio.

## Get current job status

```http
GET /api/v1/transcriptions/{job_id}
```

Spring forwards the exact UUID and preserves:

```text
job_id
status
filename
size_bytes
audio_sha256
saxophone_type
input_mode
```

Malformed UUIDs return `400 INVALID_JOB_ID`; unknown jobs return `404 TRANSCRIPTION_NOT_FOUND`; unavailable or incompatible upstream responses become controlled 502 errors. Spring adds no status transitions.

## Read transcription notes

```http
GET /api/v1/transcriptions/{job_id}/review
```

Spring validates and forwards the complete SAX-042 review snapshot, including schema/policy versions, saxophone, threshold, confidence interpretation/method, summary, ordered concert/written MIDI, timing, velocity, confidence, and low-confidence markers.

A known job without a registered result returns `409 TRANSCRIPTION_RESULT_NOT_READY`. Spring neither reconstructs nor stores review events.

## Immutable transcription revisions

SAX-043 exposes:

```http
GET  /api/v1/transcriptions/{job_id}/revisions
GET  /api/v1/transcriptions/{job_id}/revisions/{revision_number}
POST /api/v1/transcriptions/{job_id}/revisions
POST /api/v1/transcriptions/{job_id}/revisions/{revision_number}/regeneration-requests
```

Architecture:

```text
TranscriptionRevisionController
→ revision use cases
→ TranscriptionRevisionGateway
→ FastApiTranscriptionRevisionClient
```

The revision gateway is separate from upload, status, and read-review gateways.

Revision creation forwards one exact JSON command containing `base_revision_number` and ordered `update`, `add`, or `delete` operations. Spring validates the public envelope and operation shape; FastAPI validates event identity, instrument pitch, timing, provenance, history sequence, and optimistic concurrency authoritatively.

Spring accepts only complete compatible responses. It validates:

```text
job/revision identity
sequential history and parent chain
stable source-/human-UUID event IDs
model/human provenance
concert/written MIDI and velocity 0..127
finite onset and offset
model confidence or human null confidence
summary counts
schema 1.0
derived-artifact state
```

Error mapping:

```text
400 INVALID_JOB_ID
404 TRANSCRIPTION_NOT_FOUND
404 REVISION_NOT_FOUND
409 TRANSCRIPTION_RESULT_NOT_READY
409 REVISION_CONFLICT
422 INVALID_REVISION_OPERATION
422 INVALID_REVISION_EVENT
502 AI_SERVICE_ERROR
502 AI_SERVICE_UNAVAILABLE
```

No raw FastAPI body, HTML, hostname, stack trace, or path is exposed.

See:

- [`docs/contracts/transcription-revisions-gateway-v1.md`](docs/contracts/transcription-revisions-gateway-v1.md)
- [`docs/tdd/iteration-004.md`](docs/tdd/iteration-004.md)

## Explicit regeneration request

A successful request returns HTTP 202 only after Spring validates:

```text
status = REQUESTED
requested_artifacts = midi, musicxml, svg
```

Spring does not execute MIDI, MusicXML, or SVG exporters and returns no artifact bytes, completion status, percentage, or ETA.

Editing, validation and revision history are implemented.

A regeneration request is recorded explicitly.

Artifact execution remains pending.

## CORS

Only `SAXO_FRONTEND_ORIGIN` may use POST, GET, and OPTIONS on the transcription API. Credentials are disabled and wildcard origins are not used.

## Security and boundaries

- filenames are sanitized to basenames;
- only `.mp3` and `.wav` are accepted case-insensitively;
- MIME type is forwarded but not trusted as the format authority;
- audio, multipart bodies, internal URLs, upstream HTML, stack traces, and local paths are not exposed;
- Spring calculates no replacement SHA-256;
- every read/edit/request reaches FastAPI once;
- there is no Spring repository, database, cache, filesystem storage, object storage, queue, worker, retry loop, autosave, WebSocket, SSE, authentication, artifact execution, download, SAX-044, or later story.

Earlier contracts:

- [`docs/contracts/audio-upload-gateway-v1.md`](docs/contracts/audio-upload-gateway-v1.md)
- [`docs/contracts/job-status-gateway-v1.md`](docs/contracts/job-status-gateway-v1.md)
- [`docs/contracts/transcription-review-gateway-v1.md`](docs/contracts/transcription-review-gateway-v1.md)
- [`docs/tdd/iteration-001.md`](docs/tdd/iteration-001.md)
- [`docs/tdd/iteration-002.md`](docs/tdd/iteration-002.md)
- [`docs/tdd/iteration-003.md`](docs/tdd/iteration-003.md)
