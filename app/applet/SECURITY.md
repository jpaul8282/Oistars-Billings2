# Security Policy

## Overview
**Oistars Billings** is an offline-first merchant invoicing and client billing ledger application engineered for European small businesses and artisans. Data sovereignty, confidentiality, and integrity are core pillars of our security architecture.

---

## Supported Versions

We provide security updates and patches for the current release versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Security Architecture & Core Safeguards

### 1. Application Sandbox Confinement
- **Internal Storage Isolation**: All merchant records, client tax identifiers, invoices, and payments reside strictly inside Android's private internal application sandbox (`/data/data/com.aistudio.oistars.billing`).
- **No Shared External Storage**: The app does not request `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`. Media handling strictly utilizes the Android 13+ zero-permission Photo Picker (`PickVisualMedia`).
- **Scoped Cache Lifecycle**: Generated PDF invoices are produced in isolated cache storage (`context.cacheDir/exported_invoices`) and shared via Android `FileProvider` with temporary read grants (`FLAG_GRANT_READ_URI_PERMISSION`).

### 2. System Integrity & Zero Dynamic Code Loading (DCL)
- **Zero Remote Executables**: In accordance with Google Play System Integrity policies, this application never downloads, runs, or reflects upon executable bytecode (`.dex`, `.jar`, `.so`) from remote or third-party servers.
- **Static Compilation**: All business rules, VAT computations, and UI components are compiled ahead-of-time with R8 optimization and code shrinking.

### 3. Data Integrity & SQL Injection Defense
- **Compile-Time Verified Queries**: Persistence is managed through Android Jetpack Room with Kotlin Symbol Processing (KSP). All SQL statements are parameterized and verified at compile time, eliminating SQL injection vulnerabilities.
- **Type-Safe Navigation**: Screen transitions and view state serialization strictly use `kotlinx.serialization` type-safe route keys.

### 4. Zero Tracking & Telemetry Policy
- **No Advertising SDKs**: The application contains 0 advertising libraries, tracking pixels, or cross-app identifiers.
- **Zero Third-Party Data Transmission**: Invoices, customer names, bank accounts (IBAN/BIC), and billing ledgers are never uploaded, sold, or proxied to external servers.

### 5. Access Control & Protective UI
- **Biometric & PIN Safeguards**: Configurable biometric and passkey lock verification for sensitive merchant settings and destructive actions.
- **Explicit Erasure Confirmation**: Irreversible deletions (clients, invoices, database resets) require secondary modal confirmation.

---

## Reporting a Vulnerability

We take reports of security vulnerabilities seriously and appreciate the efforts of security researchers and community members.

### Disclosure Process
1. **Report Submission**: If you discover a security vulnerability, please email **security@oistars.nl** or create a private security advisory.
2. **Details to Include**:
   - Description of the vulnerability and its potential impact.
   - Exact steps or proof-of-concept (PoC) to reproduce the issue.
   - Android OS version and device model tested.
3. **Response Timeline**:
   - **Acknowledgment**: Within 48 business hours.
   - **Assessment & Status Updates**: Within 5 business days.
   - **Patch Release**: Critical vulnerabilities will be patched in a prioritized hotfix release.

### Safe Harbor
We consider security research conducted under the following terms to be authorized:
- You make a good-faith effort to avoid privacy violations, destruction of user data, and interruption of services.
- You do not exploit a vulnerability beyond what is necessary to prove its existence.
- You provide us reasonable time to resolve the issue before disclosing it publicly.
