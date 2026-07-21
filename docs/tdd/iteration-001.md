# TDD iteration 001 — SAX-040 product audio upload gateway

## Scope

SAX-040 creates the Spring Boot product boundary for the existing FastAPI upload endpoint.

```text
priority:   P1
estimate:   5 points
epic:       E4 — Product and human review
```

Only the initial HTTP 202 job creation flow is implemented. SAX-041 and later stories remain untouched.

## Empty repository bootstrap

The GitHub repository initially had no commits and reported `size: 0`. A feature branch could not exist until one root commit was created. The connector cannot create a parentless multi-file commit, so the authorized direct bootstrap contained only README documentation:

```text
79651aabe14e3dd80d75345741f94beb3dc761fc
chore: bootstrap InstrumentalSW Backend repository
```

`.gitignore`, `.editorconfig`, dependencies, tests, source, workflow, and configuration were added only after `feature/SAX-040-audio-upload` was created.

## Harness before RED

```text
2f9fcccbb186b68e165f19b573b2fb75388feabd
build(SAX-040): establish backend test harness
```

The harness fixed Java 21, Spring Boot 3.5.16, Maven 3.9.16, JUnit 5, Spotless, Checkstyle, JaCoCo, and the normal GitHub Actions gate. It contained no controller, service, client, gateway, or product behavior.

## RED

Tests-only commits preceded production:

```text
f4769864824ed06f90bc6bfe43eae5d1f1eb8304
  test(SAX-040): define upload application contract

daf9e617b029bc3aae59261406f289353e7d7f7a
  test(SAX-040): define product upload API contract

263f2ce1a873edc121bc02673d8c1b351da66fb3
  test(SAX-040): define FastAPI multipart forwarding and error mapping
```

The draft PR opened at the tests-only head. The normal workflow installed Java and Maven successfully and then failed in test compilation because the following product contracts did not exist:

```text
SubmitTranscription
TranscriptionGateway
TranscriptionUpload
TranscriptionUploadController
FastApiTranscriptionClient
domain enums and response contracts
configuration and public error handler
```

Evidence:

```text
Quality #1
run: 29856758697
job: 88722899956
result: failure during Verify after Java and Maven setup
```

No dependency-installation error was used as RED evidence.

## GREEN

Production was introduced after RED in inward-facing order:

```text
domain error codes, enums, exception, and validated job
immutable application upload command and gateway port
SubmitTranscription validation and normalization
configurable RestClient FastAPI adapter
configuration, CORS, multipart limits
public controller, response DTO, and error envelope
```

The application use case validates before the gateway, strips client path components, accepts `.mp3` and `.wav` case-insensitively, preserves content type and exact enum values, clones bytes, and does not persist.

The infrastructure test uses a real JDK HTTP server and inspects the emitted multipart body rather than verifying only a mock call.

## REFACTOR and diagnostics

The first GREEN compile exposed checked `IOException` boundaries in Spring `ClientHttpResponse` and Jackson byte-array parsing. Both were translated to controlled upstream errors.

The first completed test run exposed two test-harness issues:

```text
multipart inspection matched filename= as a fourth name
WebMvcTest required explicit CorsProperties registration
```

The regex was restricted to `Content-Disposition` field names and configuration properties were registered through `@EnableConfigurationProperties`.

The pinned formatter was applied to production and test sources. The final workflow was restored to:

```text
permissions: contents: read
checkout: pull request merge ref
command: ./mvnw verify
```

The normal workflow retains `verify.log` only as an external failure artifact. No generated log is committed.

## Test coverage

The completed suite covers:

- valid WAV and MP3 in lowercase and uppercase;
- exact public multipart names;
- normalized basename without client path leakage;
- missing, empty, unsupported, invalid instrument, and invalid mode requests;
- complete HTTP 202 response preservation;
- stable public JSON errors;
- one gateway call with unchanged bytes and enums;
- real upstream multipart method, path, names, filename, content, MIME, instrument, and mode;
- upstream 400, 413, 415, 422, 500, and 503;
- timeout and refused connection;
- malformed JSON, invalid UUID, invalid SHA, and unsafe response filename;
- domain invariants, defensive copies, configuration normalization, size-limit and malformed-multipart envelopes.

The final suite contains 37 tests. Spotless and Checkstyle pass, and JaCoCo exceeds the required 90% line threshold. Exact final workflow and coverage evidence is recorded in the draft PR body after the documented head is validated.

## Contract coherence

The Backend tests demonstrate the same contract already present in the AI repository:

```text
file
saxophone_type
input_mode

soprano | alto | tenor | baritone
solo | mixture
```

No shared package or second contract was created.

## Boundaries

No audio is persisted. No replacement SHA-256 is calculated. The service does not expose its internal FastAPI URL, raw upstream body, stack trace, multipart, or audio. It does not implement authentication, database, storage, queue, worker, retries, polling, WebSocket, SSE, processing states, SAX-041, or later stories.
