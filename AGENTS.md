# Academy Client agent instructions

## Project role

This is the Kotlin Multiplatform mobile client for an online academy platform. Android and iOS share networking, session, domain, and application logic through the `shared` module. The server is a WordPress site exposing a curated REST API; this repository contains only the mobile client.

## Architecture rules

- Put portable logic in `shared/src/commonMain`.
- Use `androidMain` and `iosMain` only for platform implementations such as Keystore, Keychain, HTTP engines, and platform UI integration.
- UI code must call view models/use cases, not Ktor directly.
- API access must go through `AcademyApi`, and authenticated operations through `AcademySessionManager.executeAuthenticated`.
- Keep WordPress, LearnDash, WooCommerce, and media-host implementation details out of the app; the client consumes curated DTOs only.
- Represent WordPress IDs as Kotlin `Long` values.
- Keep production API configuration disabled until explicitly approved.
- Write new and modified user-facing app copy in broadly understood Latin American Spanish. Use the shared string-resource mechanism for UI copy; do not add new inline English labels or errors.
- Preserve server-provided course, lesson, quiz, and answer content as authored, and never translate protocol values, API fields, or identifiers.

## Security rules

- Store access and refresh tokens only through the platform-secure storage abstractions.
- Never log credentials, tokens, cookies, media keys, or private learner data.
- Development HTTP logging is opt-in and must keep authorization headers sanitized.
- Keep live local/staging credentials in environment variables or a local secret store, never in source or tests.

## Verification

Use `scripts/agent-check.sh` as the default deterministic harness. It runs the diff validation, a credential-pattern scan, and the shared Android-host tests. See the repository README for broader commands. When the change affects native code, also run the applicable Android device or iOS/macOS verification when available.

Before committing, check:

```bash
git diff --check
```

Update `ARCHITECTURE.md` when the design, session lifecycle, or API surface changes.
