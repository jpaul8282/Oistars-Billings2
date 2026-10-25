# Security Policy & Vulnerability Reporting

Oistars Billings is dedicated to enterprise-grade security, data sovereignty, and open-source supply chain integrity in compliance with the **OpenSSF (Open Source Security Foundation) Best Practices**.

---

## Supported Versions

Security patches and vulnerability updates are actively backported to supported releases:

| Version | Status | Security Support Level |
|---|---|---|
| `5.x.x` | **Current Active** | Full security patches, dependency audits, and automated CVE remediation |
| `4.x.x` | **Maintenance** | Critical security patches only |
| `< 4.0.0` | **End of Life (EOL)** | Unsupported; users must upgrade |

---

## Reporting a Vulnerability

We treat all security reports with utmost urgency and follow **Coordinated Vulnerability Disclosure (CVD)** principles.

### Private Reporting Channels
If you discover a security vulnerability, **do NOT open a public GitHub issue**. Instead, please report it privately:

- **Primary Security Email**: `security@oistars.nl`
- **Lead Maintainer Email**: `westerveldjp@gmail.com`
- **PGP Encryption**: Security reports may be encrypted using our maintainer PGP key (available upon request via email).

### Information to Include in Your Report
To accelerate validation and triage, please provide:
1. Type of issue (e.g., buffer overflow, SQL injection, privilege escalation, authentication bypass, cryptographic weakness).
2. Affected version(s) and commit SHA.
3. Step-by-step reproduction instructions or a minimal Proof of Concept (PoC).
4. Potential business or user impact.

---

## Response Timeline & Service Level Agreement (SLA)

| Milestone | Target Response Time | Action Taken |
|---|---|---|
| **Initial Acknowledgment** | **Within 24 hours** | Maintainer confirms receipt and opens an internal tracking ticket. |
| **Triage & Validation** | **Within 72 hours** | Vulnerability reproduced, severity determined via CVSS v3.1 scoring. |
| **Fix Development & Patch** | **Within 7 days** | Fix implemented, covered by automated Robolectric unit tests. |
| **Release & Disclosure** | **Coordinated (typically 14–30 days)** | Release of updated APK / AAB and publication of security advisory. |

---

## Security Invariants & Zero-Trust Architecture

Oistars Billings is engineered with layered defenses:

1. **Zero Dynamic Code Loading (DCL)**:
   - Dynamic class loading (`DexClassLoader`, reflection-based execution of untrusted DEX/JAR files) is strictly forbidden.
   - All code is compiled into signed Android release artifacts.

2. **ProGuard & R8 Code Hardening**:
   - Release builds enforce byte-code obfuscation, dead-code elimination, and resource shrinking.
   - Core financial logic and SQLite entities are protected against reverse engineering.

3. **Isolated SQLite Sandbox**:
   - Customer records, invoices, and client data are stored in a private Android sandbox database (`/data/data/com.oistars.billings/databases/`).
   - All queries use parameterized Room PreparedStatements, preventing SQL injection.

4. **Cryptographic Standards**:
   - Network communication exclusively uses TLS 1.3 with standard HTTPS certificates.
   - Zero hardcoded API keys in source control; runtime credentials utilize the Secrets Gradle Plugin and environment injection.

5. **Cloud Security Rules**:
   - Firebase Cloud Firestore access enforces zero-trust document ownership checks (`isOwner(userId)`).
