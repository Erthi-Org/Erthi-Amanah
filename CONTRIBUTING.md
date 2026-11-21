# Contributing — Erthi Amanah

Thank you for considering contributing to Erthi Amanah.

## Ground Rules

1. This is a charitable, privacy-first project. Contributions must not introduce advertising SDKs, analytics trackers, or mandatory cloud accounts.
2. All personal data must remain stored locally on-device by default.
3. No features that require user registration.
4. Security-sensitive changes require extra review.

## Development Setup

```bash
git clone https://github.com/Erthi-Org/Erthi-Amanah.git
cd Erthi-Amanah
# Open in Android Studio or build from CLI
./gradlew assembleDebug
```

**Requirements:**
- Android Studio Ladybug (2024.2+) or later
- JDK 17
- Android SDK with API 35 and build-tools 35.0.0

## Branching

- `main` — stable releases only, protected
- `feature/<name>` — new features
- `fix/<name>` — bug fixes
- `docs/<name>` — documentation only

## Submitting a Pull Request

1. Create a feature branch from `main`
2. Write or update tests for your change
3. Run `./gradlew testDebugUnitTest lintDebug`
4. Open a PR with a clear description of what and why
5. Never merge into `main` without review

## Code Style

- Kotlin official style guide
- Compose: stateless composables where possible, state in ViewModels
- No hardcoded Arabic strings — use `strings.xml`
- No sensitive data in logs (`Log.d`, `Log.e`, etc.)

## Security Contributions

If you find a security issue, please report it privately via GitHub Issues with the `security` label before disclosing publicly.
