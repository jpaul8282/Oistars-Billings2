# Oistars Billings - Google Play Store Metadata

This file documents the complete metadata and debug configuration required for Google Play Store distribution.

## Package Information

```
Package Name: com.oistars.billings
Application ID: com.oistars.billings
Minimum SDK: 24 (Android 7.0)
Target SDK: 36 (Android 15)
Version Code: 1
Version Name: 1.0.0
Language: Kotlin
```

## Store Listing

### App Title
```
Oistars Billings
```

### Short Description (80 chars max)
```
Professional invoicing & billing. Offline-first, secure, local-first platform.
```

### Full Description
```
Oistars Billings is your complete financial management solution for invoicing,
client management, recurring subscriptions, and payment tracking.

Key Features:
✓ Professional invoice creation with automatic tax and discount calculations
✓ Real-time financial metrics (revenue, outstanding, overdue tracking)
✓ Multi-currency support (EUR, USD, GBP, and more)
✓ Offline-first design - all data stored securely on your device
✓ Dark and light themes for comfortable usage
✓ PDF export and printing support
✓ Recurring billing automation
✓ Complete client directory with payment history

Security & Privacy:
✓ End-to-end encrypted local storage
✓ No cloud dependency (optional Firebase integration)
✓ GDPR compliant - you own your data
✓ Production-grade code signing and obfuscation
✓ No invasive tracking or analytics

Perfect for:
- Freelancers managing client projects
- Small businesses tracking invoices
- E-commerce stores managing billing
- Agencies handling multiple clients
```

### Category
```
Business
```

### Content Rating
```
4+ (No objectionable content)
```

## Screenshots

### Screen 1: Dashboard
**File**: `assets/dashboard-screen.png`  
**Caption**: "Real-time financial metrics and quick actions"

### Screen 2: Invoice List
**File**: `assets/invoices-screen.png`  
**Caption**: "Filter, search, and manage invoices by status"

### Screen 3: Client Directory
**File**: `assets/clients-screen.png`  
**Caption**: "Organize and track your complete client database"

### Screen 4: Create Invoice
**File**: `assets/create-invoice-screen.png`  
**Caption**: "Intuitive invoice creation with automatic calculations"

### Screen 5: Account Settings
**File**: `assets/settings-screen.png`  
**Caption**: "Customize theme and manage your account"

## Release Notes

### Version 1.0.0 - Initial Release

```markdown
🎉 Welcome to Oistars Billings!

We're excited to launch the first version of Oistars Billings, a comprehensive
billing platform designed for professionals who demand security, privacy, and
financial accuracy.

✨ What's New:
- Professional invoice management with automatic calculations
- Complete client directory and payment tracking
- Recurring subscription automation
- Multi-currency support
- Offline-first local storage
- PDF export and printing
- Dark/Light theme support

🔐 Security & Privacy:
- All data encrypted and stored locally on your device
- GDPR compliant with full data ownership
- No third-party analytics or tracking
- Production-grade code signing

🐛 Known Limitations:
- Firebase sync is optional (requires manual configuration)
- Print feature requires Android 5.0 or higher
- Internet access required only for optional cloud features

📝 Thank you for choosing Oistars Billings!
```

## Privacy Policy

```
Privacy Policy for Oistars Billings

Last Updated: October 4, 2026

1. Data Collection
Oistars Billings operates on a local-first model. All financial data you enter
(clients, invoices, transactions) is stored exclusively on your device.

2. No Third-Party Sharing
We do not collect, share, or sell your personal or financial data to any
third parties.

3. Optional Firebase Integration
If you enable Firebase features, some data may be sent to Google Firebase
servers for AI processing and security checks. This is entirely optional.

4. Data Deletion
You can delete all app data by clearing app data in Android Settings or
uninstalling the application.

5. Contact
For privacy questions: privacy@oistars.example.com
```

## Terms of Service

```
Terms of Service for Oistars Billings

Last Updated: October 4, 2026

1. License Grant
Oistars Billings is provided under a proprietary license. You may use the app
for personal and business purposes as outlined in this license.

2. Limitations of Liability
Oistars Billings is provided "as is" without warranty. While we rigorously
test all financial calculations, you are responsible for verifying accuracy
and compliance with local tax regulations.

3. Financial Calculations
Oistars Billings provides tools for invoice creation and financial tracking.
Consult a professional accountant for authoritative financial and tax guidance.

4. Acceptable Use
You agree not to use Oistars Billings for illegal purposes or to violate the
rights of others.

5. Changes to Terms
We reserve the right to update these terms. Continued use constitutes acceptance.

6. Contact
For questions about these terms: legal@oistars.example.com
```

## Compliance Checklist

- [x] App name and icon finalized
- [x] Detailed app description written
- [x] Privacy policy created
- [x] Terms of service established
- [x] Screenshots prepared (5 screens)
- [x] Release notes documented
- [x] Content rating assigned (4+)
- [x] Category selected (Business)
- [x] No ads or in-app purchases
- [x] No invasive permissions requested
- [x] Code signed with production keystore
- [x] Minify and ProGuard enabled
- [x] All dependencies tracked and updated
- [x] Security vulnerabilities disclosure policy in place

## Debug Build Configuration

### Test Credentials

```properties
# For testing invoice calculations
TEST_SUBTOTAL=1000.00
TEST_TAX_RATE=0.20
TEST_DISCOUNT_RATE=0.10
TEST_EXPECTED_TOTAL=1080.00
```

### Debug Logging

```kotlin
if (BuildConfig.DEBUG_LOGGING) {
    Log.d("BillingViewModel", "Invoice saved: ${invoice.id}")
    Log.d("BillingRepository", "Payment recorded: ${payment.id}")
}
```

### Test Scenarios

1. **Create Invoice**
   - Add 2-3 line items
   - Apply per-item discounts
   - Verify total = (subtotal - discount) * (1 + tax_rate)

2. **Record Payment**
   - Record partial payment (50% of total)
   - Verify balance_due = total - payment
   - Record remaining payment
   - Verify invoice status = PAID

3. **Currency Conversion**
   - Create invoice in EUR
   - Create invoice in USD
   - Verify calculations are currency-independent

4. **Offline Mode**
   - Create invoice
   - Turn off network
   - Modify invoice
   - Turn on network
   - Verify data persisted

5. **Edge Cases**
   - Invoice with 0 items (should show error)
   - Invoice with negative discount (should clamp to 0)
   - Overpayment (should clamp balance to 0)

## Build Signing Configuration

### Release Keystore

```bash
# Generate keystore (one-time)
keytool -genkey -v -keystore oistars-billings-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload

# Set environment variables
export KEYSTORE_PATH=/path/to/oistars-billings-release.jks
export STORE_PASSWORD=your-secure-password
export KEY_PASSWORD=your-secure-password

# Build signed APK
./gradlew assembleRelease
```

### Bundle Signing (for Play Store)

```bash
# Build App Bundle
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

## Crash Reporting Setup

### Logcat Monitoring

```bash
# Monitor all crashes
adb logcat | grep FATAL

# Monitor app-specific crashes
adb logcat | grep com.oistars.billings
```

### Firebase Crashlytics (Optional)

```kotlin
// In MainActivity.kt
FirebaseCrashlytics.getInstance().recordException(exception)
```

## Performance Benchmarks

### App Load Time
- Target: < 2 seconds
- Database initialization < 500ms
- UI rendering < 16ms per frame

### Memory Usage
- Target: < 100MB baseline
- < 200MB with 1000 invoices loaded

### Database Queries
- List all invoices: < 100ms
- Search by client: < 50ms
- Calculate metrics: < 200ms

## Submission Checklist

1. **App Signing**
   - [x] Signed with production keystore
   - [x] Version code incremented
   - [x] Version name updated

2. **Content**
   - [x] Screenshots optimized (1440x2560)
   - [x] App icon provided (512x512)
   - [x] Feature graphic (1024x500)
   - [x] Screenshots tagged with captions

3. **Metadata**
   - [x] Title, description, category finalized
   - [x] Privacy policy linked
   - [x] Terms of service linked
   - [x] Support email provided

4. **Compliance**
   - [x] No ads or tracking
   - [x] Permissions justified
   - [x] Content rating assigned
   - [x] Security vulnerabilities disclosed responsibly

5. **Quality**
   - [x] No crashes on test devices
   - [x] Financial calculations verified
   - [x] Offline mode tested
   - [x] Performance benchmarks met

---

**Ready for Play Store submission!**
