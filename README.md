# InstrumentalSW Backend

Spring Boot product API for InstrumentalSW (Saxo). SAX-040 exposes the public browser-facing upload gateway and forwards accepted MP3/WAV requests to the existing internal FastAPI AI service.

```text
Next.js :3000
  → Spring Boot :8080
    → FastAPI :8000
```

The browser never calls FastAPI directly. Audio bytes are held only for the duration of the request and are not persisted.

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

The command compiles for Java 21 and runs unit tests, MVC tests, real HTTP multipart integration tests, Spotless, Checkstyle, and a JaCoCo line-coverage gate of at least 90%.

On Windows:

```powershell
mvnw.cmd verify
```

## Run

Start the existing AI service on port 8000, then run:

```bash
./mvnw spring-boot:run
```

The product API listens on:

```text
http://localhost:8080
```

## Environment variables

| Variable | Local default | Purpose |
| --- | --- | --- |
| `SAXO_AI_BASE_URL` | `http://localhost:8000` | Internal FastAPI base URL |
| `SAXO_AI_CONNECT_TIMEOUT` | `2s` | Connection timeout |
| `SAXO_AI_READ_TIMEOUT` | `30s` | Upstream response timeout |
| `SAXO_FRONTEND_ORIGIN` | `http://localhost:3000` | Allowed browser origin |
| `SAXO_MAX_MULTIPART_FILE_SIZE` | `101MB` | Product transport file barrier |
| `SAXO_MAX_MULTIPART_REQUEST_SIZE` | `102MB` | Product transport request barrier |

The multipart values are explicit transport limits. FastAPI remains authoritative for AI-service functional limits.

## Public endpoint

```http
POST /api/v1/transcriptions
Content-Type: multipart/form-data
```

Exact fields:

```text
file
saxophone_type
input_mode
```

Exact values:

```text
saxophone_type: soprano | alto | tenor | baritone
input_mode:     solo | mixture
```

Example with a synthetic or otherwise legally usable WAV:

```bash
curl --request POST http://localhost:8080/api/v1/transcriptions \
  --form 'file=@synthetic.wav;type=audio/wav' \
  --form 'saxophone_type=alto' \
  --form 'input_mode=solo'
```

Successful requests return HTTP 202 and preserve the complete FastAPI response:

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

## Security and boundaries

- filename path components are removed before forwarding;
- only `.mp3` and `.wav` are accepted, case-insensitively;
- MIME type is preserved when available but is not trusted as the format authority;
- audio, multipart bodies, internal URLs, upstream HTML, stack traces, and paths are not exposed publicly;
- the Backend does not calculate a replacement SHA-256;
- there is no authentication, persistence, database, queue, worker, retry, polling, WebSocket, or SSE in SAX-040.

See [`docs/contracts/audio-upload-gateway-v1.md`](docs/contracts/audio-upload-gateway-v1.md) and [`docs/tdd/iteration-001.md`](docs/tdd/iteration-001.md).
