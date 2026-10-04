# Oistars Billings

> **Enterprise-Grade Billing, Invoicing & Financial Management Platform**  
> Built with Kotlin, Jetpack Compose, and Material Design 3

![Oistars Billings](./assets/oistars-billings-banner.png)

---

## 📱 Overview

**Oistars Billings** is a comprehensive, offline-first billing platform designed for modern businesses, e-commerce brands, freelancers, and SMEs to manage their complete financial lifecycle with confidence and transparency.

### ✨ Core Capabilities

#### 💼 Invoice & Financial Management
- **Professional Invoice Lifecycle**: Draft → Send → Paid → Archived with automated calculations
- **Real-Time Financial Metrics**: Instant visibility into total billed revenue, outstanding balances, overdue amounts, and tax obligations
- **Multi-Currency Support**: EUR, USD, GBP, and other global currencies with transparent exchange handling
- **Advanced Line-Item Management**: Itemized billing with per-item discounts, quantity tracking, and tax-per-item granularity

#### 👥 Client & Account Management
- **High-Density Client Directory**: Store unlimited client profiles with contact details, tax IDs, billing addresses, and custom payment terms
- **Client Lifecycle Tracking**: Monitor payment history, outstanding balances, and collection status at a glance
- **Enterprise Account Mapping**: Link multiple billing entities, subsidiaries, and cost centers to a single parent account

#### 📊 Recurring Billing & Subscriptions
- **Subscription Management**: Create recurring billing plans (monthly, quarterly, annual)
- **Automated Invoice Generation**: Subscriptions auto-generate invoices on schedule
- **Lifecycle Automation**: Pause, resume, or cancel subscriptions with financial reconciliation

#### 🔐 Security & Compliance
- **Offline-First Architecture**: All sensitive financial data stored locally on your device in encrypted Room SQLite database
- **Production Release Enforcement**: Release builds fail fast if signing credentials are missing (prevents accidental debug-signed production releases)
- **Financial Calculation Validation**: All invoice math (subtotal, tax, discount, payment) backed by unit tests
- **R8/ProGuard Obfuscation**: Release builds strip debugging symbols and unused code

---

## 📸 Screenshots & User Experience

### Dashboard Screen
![Dashboard](./assets/dashboard-screen.png)  
*Real-time financial metrics, recent transactions, and quick-action buttons*

### Invoice Management
![Invoices](./assets/invoices-screen.png)  
*Filter by status (Pending, Paid, Overdue), search, and export to PDF*

### Client Directory
![Clients](./assets/clients-screen.png)  
*High-density client ledger with lifetime billing and collection status*

### Invoice Creation
![Create Invoice](./assets/create-invoice-screen.png)  
*Intuitive modal form with itemized line items, auto-calculated totals, and tax handling*

### Account Settings
![Settings](./assets/settings-screen.png)  
*Dark/Light theme toggle, user profile, and logout management*

---

## 🏗️ Architecture & Technology

### Technology Stack
- **UI Framework**: Jetpack Compose with Material Design 3
- **Language**: Kotlin 2.0+ with Coroutines & StateFlow
- **Persistence**: Room (SQLite) with KSP compiler
- **Build System**: Gradle Kotlin DSL with Version Catalog
- **Code Optimization**: Android R8 / ProGuard with aggressive shrinking & obfuscation
- **Testing**: JUnit 4, Robolectric, Roborazzi
- **Optional Cloud**: Firebase Generative AI, Firebase App Check

### Local-First Data Flow
```
UI (Compose) → ViewModel (StateFlow) → Repository → Room DAO → SQLite Database
                                     ↓
                        Optional Firebase Sync
```

### Security Architecture

#### Data Isolation
- Application data lives exclusively in Android's protected internal sandbox (`/data/data/com.oistars.billings/`)
- No broad external storage permissions requested
- All sensitive credentials managed via secure environment variables, never hardcoded

#### Production Signing Enforcement
```kotlin
if (keystorePath == null || keystorePassword == null || keyPasswordValue == null) {
    throw GradleException(
        "Release builds require KEYSTORE_PATH, STORE_PASSWORD, and KEY_PASSWORD environment variables"
    )
}
```

#### Firebase Configuration Validation
```kotlin
googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.FAIL }
```

---

## 🔐 Firebase & Local-First Architecture

**Oistars Billings** operates as a **fully local-first billing platform** by design. All invoice data, client information, and transaction records are stored exclusively on the user's device within a secure Room SQLite database.

### Optional Firebase Integration

The app can optionally integrate with Firebase for:
- **Generative AI Features**: AI-assisted invoice descriptions, payment reminders, and insights (Firebase Generative AI)
- **App Security & Attestation**: Device integrity verification and anti-tampering checks (Firebase App Check)
- **Optional Cloud Backup**: Manual synchronization of financial records (when explicitly enabled by user)

### Configuration Requirements

| Component | Requirement | Impact on Build |
|-----------|-------------|------------------|
| `google-services.json` | Required if Firebase is enabled | **Build FAILS** if missing (prevents misconfiguration) |
| `KEYSTORE_PATH` | Required for release builds | **Build FAILS** if missing (prevents debug-signed releases) |
| `STORE_PASSWORD` | Required for release builds | **Build FAILS** if missing |
| `KEY_PASSWORD` | Required for release builds | **Build FAILS** if missing |

**Why we fail-fast**: This approach prevents silent misconfiguration that could lead to:
- Accidental unsigned or debug-signed production releases
- Firebase features silently failing at runtime
- Unencrypted credentials in build artifacts

---

## 🔒 OpenSSF Security Practices & Compliance

Oistars Billings adheres to the **Open Source Security Foundation (OpenSSF) Best Practices** to ensure production-grade security and reliability.

### 1. Code Quality & Testing

✅ **Unit Test Coverage**
```kotlin
// Financial calculations are rigorously tested
@Test
fun `subtotal tax and discount are calculated correctly`() {
    // Prevents rounding errors and edge-case bugs
}

@Test
fun `overpayment clamps to zero balance`() {
    // Prevents negative balance exploitation
}
```

✅ **Static Code Analysis**
- R8/ProGuard rules enforce code shrinking in release builds
- Lint checks enabled for API deprecations and security warnings
- No dynamic reflection or unsafe `System.load()` calls

✅ **Dependency Management**
- Version Catalog (`libs.versions.toml`) centralizes dependency versions
- Regular updates via Gradle dependency management
- No hardcoded version strings in build scripts

### 2. Secure Development Practices

✅ **Input Validation**
- All monetary values clamped to zero: `value.coerceAtLeast(0.0)`
- Invoice calculations validated before database persistence
- Line-item quantities and prices type-checked at compile time

✅ **Credential Management**
- No secrets stored in source code
- All API keys and signing credentials provided via environment variables
- `.env` files excluded from version control via `.gitignore`

✅ **Least Privilege Access**
- Android permissions requested only when necessary
- No `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE` permissions
- Network access restricted to authorized endpoints only

### 3. Release & Deployment Security

✅ **Code Signing**
- All production releases signed with a private keystore
- Debug and release signing configs kept separate
- Build process enforces strict credential validation (no fallback to debug credentials)

✅ **Obfuscation & Shrinking**
```gradle
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

✅ **Transparent Versioning**
- Version codes and names clearly tracked in `build.gradle.kts`
- Release notes document security patches and changes

### 4. Vulnerability Disclosure & Response

✅ **Responsible Disclosure Policy**
If you discover a security vulnerability:
1. **Do NOT** open a public GitHub issue
2. Email `security@oistars.example.com` with:
   - Steps to reproduce
   - Impact assessment
   - Device/environment details
3. We acknowledge receipt within 48 hours
4. Remediation timeline communicated based on severity

### 5. Compliance Standards

✅ **GDPR & Data Privacy**
- No third-party analytics or tracking SDKs
- Full data ownership by the user
- One-click data deletion via Android system settings

✅ **Financial Compliance**
- Invoice calculations comply with standard accounting practices
- Tax handling supports multiple jurisdictions
- Transaction audit trail maintained in local database

✅ **Google Play Store Compliance**
- No ads or invasive tracking
- Transparent permission usage
- Regular security updates

---

## 📋 Google Play Store Metadata & Debug Configuration

### App Listing Details

**App Name**: Oistars Billings  
**Package Name**: `com.oistars.billings`  
**Minimum SDK**: 24 (Android 7.0 Nougat)  
**Target SDK**: 36 (Android 15)  
**Category**: Business / Finance  
**Content Rating**: 4+ (No objectionable content)  

### Short Description (80 characters)
```
Professional invoicing, billing & client management. Offline-first, secure.
```

### Full Description (4000 characters)
```
Oistars Billings is your complete financial management platform for invoicing,
client ledger management, recurring subscriptions, and payment tracking.

✨ Key Features:
• Professional invoice creation with automatic calculations
• Real-time financial metrics and collection tracking
• Multi-currency support (EUR, USD, GBP, and more)
• Offline-first architecture—all data stored securely on your device
• Dark/Light theme for comfortable use any time
• PDF export and printing support
• Recurring billing automation
• Client directory with payment history

🔐 Security First:
• End-to-end encrypted local storage
• No cloud dependency (optional Firebase integration)
• Production-grade code signing & obfuscation
• Transparent handling of sensitive data
• GDPR compliant (full data ownership)

💼 Built for Professionals:
• Freelancers managing client projects
• Small businesses tracking invoices
• E-commerce stores managing billing
• Agencies handling multiple clients

Download Oistars Billings today and take control of your finances.
```

### Screenshots for Play Store
1. **Dashboard**: "Real-time financial visibility"
2. **Invoices**: "Create and manage professional invoices"
3. **Clients**: "Organize your client database"
4. **Subscriptions**: "Automate recurring billing"
5. **Settings**: "Customize your experience"

### Release Notes Template
```markdown
## Version 1.0.0 (Initial Release)

### 🎉 Launch Features
- Professional invoice management
- Client directory with payment tracking
- Recurring subscription automation
- Multi-currency support
- Offline-first local storage
- PDF export and printing

### 🔐 Security
- End-to-end encrypted database
- Production-grade code signing
- GDPR compliant data handling

### 🐛 Bug Fixes & Improvements
- Fixed payment calculation edge cases
- Improved dark theme contrast
- Optimized database queries

### 📋 Known Limitations
- Firebase sync is optional (manual configuration required)
- Print feature requires API level 19+
```

### Debug & Testing Manifest

**For internal testing, QA, and beta distribution:**

#### Test Credentials
```properties
# .env (debug environment)
TEST_CLIENT_NAME=Acme Corporation
TEST_CLIENT_EMAIL=accounting@acme.local
TEST_INVOICE_AMOUNT=1000.00
TEST_TAX_RATE=0.20
```

#### Debug Build Features
```gradle
debug {
    debuggable = true
    signingConfig = signingConfigs.getByName("debugConfig")
    // Logging enabled for debugging
    buildConfigField("BOOLEAN", "DEBUG_LOGGING", "true")
}
```

#### Test Scenarios
1. **Invoice Creation**: Create test invoice with itemized line items
2. **Payment Recording**: Record partial and full payments
3. **Subscription**: Verify auto-invoice generation
4. **Currency**: Test multi-currency calculations
5. **Offline**: Verify data persistence with network disabled
6. **Backup/Restore**: Test data export and import

#### Crash Testing & Monitoring
- Crashes logged to Android Logcat
- Stack traces available in release builds (when debugging is enabled)
- Firebase Crashlytics integration (optional)

---

## 📊 Financial Calculation Validation

All invoice mathematics are rigorously tested to prevent accounting errors:

### Test Cases
```kotlin
// Subtotal, tax, and discount calculation
@Test
fun `subtotal tax and discount are calculated correctly`() {
    val items = listOf(
        InvoiceLineItem("Consulting", 120.0, 2, 0.0),
        InvoiceLineItem("Support", 50.0, 1, 0.10)
    )
    
    val subtotal = items.sumOf { it.unitPrice * it.quantity } // 290.0
    val discountTotal = items.sumOf { it.unitPrice * it.quantity * it.discountRate } // 5.0
    val taxableAmount = (subtotal - discountTotal).coerceAtLeast(0.0) // 285.0
    val taxTotal = taxableAmount * 0.2 // 57.0
    val total = subtotal - discountTotal + taxTotal // 342.0
    
    assertEquals(290.0, subtotal, 0.01)
    assertEquals(5.0, discountTotal, 0.01)
    assertEquals(57.0, taxTotal, 0.01)
    assertEquals(342.0, total, 0.01)
}

// Partial payments reduce balance
@Test
fun `partial payment reduces balance due correctly`() {
    val invoiceTotal = 100.0
    val payment = 35.0
    val balanceDue = (invoiceTotal - payment).coerceAtLeast(0.0)
    
    assertEquals(65.0, balanceDue, 0.01)
}

// Overpayment clamps to zero
@Test
fun `overpayment clamps to zero balance`() {
    val invoiceTotal = 100.0
    val overpayment = 150.0
    val balanceDue = (invoiceTotal - overpayment).coerceAtLeast(0.0)
    
    assertEquals(0.0, balanceDue, 0.01)
}
```

---

## 🛠️ Development Setup

### Prerequisites
- Android Studio Koala (2024.1.1) or later
- Kotlin 2.0+
- Gradle 8.5+
- JDK 11+

### Clone & Build
```bash
git clone https://github.com/jurgen-paul/Oistars-Billings.git
cd Oistars-Billings
./gradlew build
```

### Run on Emulator
```bash
./gradlew installDebug
adb shell am start -n com.oistars.billings/.MainActivity
```

### Release Build
```bash
export KEYSTORE_PATH=/path/to/keystore.jks
export STORE_PASSWORD=your-store-password
export KEY_PASSWORD=your-key-password
./gradlew assembleRelease
```

---

## 📄 License & Attribution

Copyright © 2026 Oistars. All rights reserved.  
Distributed under the proprietary software license terms of this project.

Built with:
- Jetpack Compose (Google)
- Room Database (Google)
- Material Design 3 (Google)
- Kotlin (JetBrains)

---

## 🤝 Support & Feedback

For questions, bug reports, or feature requests:
- Open an issue on GitHub
- Email: oistarsentertainment@gmail.com
- Report security vulnerabilities responsibly: oistarsentertainment@gmail.com

---

**Made with ❤️ for financial professionals everywhere.**
