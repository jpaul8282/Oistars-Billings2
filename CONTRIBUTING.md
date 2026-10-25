# Contributing to Oistars Billings

Thank you for your interest in contributing to **Oistars Billings**! This project adheres to the **OpenSSF Best Practices** criteria.

---

## Code of Conduct

All contributors and maintainers are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please read it before participating in our community.

---

## How to Contribute

### 1. Reporting Bugs
- Search existing issues and pull requests to avoid duplicates.
- If you find a new bug, submit an issue detailing:
  - Android OS version, device model, and app version (`versionName` / `versionCode`).
  - Clear steps to reproduce the behavior.
  - Expected vs. actual behavior.
  - Logcat traces (strip any personal financial data before posting).
- **Security Vulnerabilities**: Do **NOT** report security bugs in public issues. Refer to [SECURITY.md](SECURITY.md) for private reporting channels.

### 2. Feature Requests
- Open a feature request discussion explaining the user value, use case, and proposed UI/API interface.

### 3. Submitting Pull Requests (PRs)
1. **Fork & Branch**: Fork the repository and create a feature branch (`git checkout -b feature/my-new-feature` or `fix/issue-description`).
2. **Follow Coding Standards**:
   - Write idiomatic Kotlin and declarative Jetpack Compose UI.
   - Adhere to Material Design 3 guidelines.
   - Touch targets must be at least 48dp (`minimumInteractiveComponentSize`).
   - Every user-facing interactive component must include a descriptive `Modifier.testTag("...")`.
3. **Automated Testing**:
   - Every new feature or bugfix MUST include unit / Robolectric test coverage in `app/src/test/java/com/example/`.
   - Ensure all tests pass:
     ```bash
     gradle :app:testDebugUnitTest
     ```
4. **Build Verification**:
   - Ensure the release bundle builds without errors or warnings:
     ```bash
     gradle :app:bundleRelease
     ```
5. **Commit Messages**:
   - Use concise conventional commits (e.g., `feat: add revenue chart interactive scrubber`, `fix: resolve VAT calculation rounding edge case`).

---

## Development Environment Setup

- **IDE**: Android Studio Ladybug | 2024.2.1+ or Google AI Studio
- **JDK**: Java 17 / OpenJDK 17
- **Android SDK**: API 36 (targetSdk: 36, minSdk: 24)
- **Gradle**: 8.9+ (use system `gradle`, do NOT run `gradle clean`)

---

## Developer Certificate of Origin (DCO) & Licensing

By contributing to this repository, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE) in accordance with Section 5 of the License.
