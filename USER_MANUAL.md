# 📘 Oistars Billings - Official User Manual & Store Operations Guide

> **Enterprise Financial Management, Invoicing, Client Store Ledger & Instant Bankwire Platform**  
> **Application Package**: `com.oistars.billings`  
> **Target Audience**: Business Owners, E-Commerce Brands, Freelancers, Financial Controllers, Google Play Store Reviewers  
> **Lead Billing Owner**: Jurgen Paul Westerveld (`westerveldjp@gmail.com`)

---

## 📑 Table of Contents
1. [Introduction & Platform Overview](#1-introduction--platform-overview)
2. [Visual Store Showcase & Screen Previews](#2-visual-store-showcase--screen-previews)
3. [Financial Dashboard & Real-Time Analytics](#3-financial-dashboard--real-time-analytics)
4. [Digital Invoicing & PDF Export](#4-digital-invoicing--pdf-export)
5. [Client Store Ledger & Cloud Sync](#5-client-store-ledger--cloud-sync)
6. [Bankwire & Wero Instant Payout System](#6-bankwire--wero-instant-payout-system)
7. [Recurring Retainers & Subscriptions](#7-recurring-retainers--subscriptions)
8. [Account Settings & Merchant Configuration](#8-account-settings--merchant-configuration)
9. [ProGuard Security & OpenSSF Compliance](#9-proguard-security--openssf-compliance)
10. [Troubleshooting & Support Channels](#10-troubleshooting--support-channels)

---

## 1. Introduction & Platform Overview

**Oistars Billings** is designed from the ground up for high reliability, data sovereignty, and speed. Built with modern Android architecture (Jetpack Compose, Kotlin Coroutines, StateFlow, Room SQLite, and Firebase Cloud Firestore), it operates on a **Local-First principle**:
- **Offline Reliability**: You can create invoices, manage client contacts, compute VAT rates, and schedule recurring billing even without internet access.
- **Secure Cloud Synchronization**: Gated behind Google Identity with Jetpack Credential Manager, records synchronize with Google Cloud Firestore (`europe-west2`) with zero-trust access control.
- **High-Density Usability**: Optimized for both single-hand smartphone use and tablet/foldable layouts according to Material Design 3 guidelines.

---

## 2. Visual Store Showcase & Screen Previews

The following 5 promotional content images highlight the key user flows across the application:

| Feature Area | Description | Preview Asset |
|---|---|---|
| **1. Financial Dashboard** | Real-time revenue overview, cash flow health indicator, and rapid invoice dispatch | `app/src/main/res/drawable/img_store_dashboard_1792326378715.jpg` |
| **2. Digital Invoices** | Multi-status filtering (Paid, Pending, Overdue), VAT summaries, and PDF export | `app/src/main/res/drawable/img_store_invoices_1792326393162.jpg` |
| **3. Wero & Bankwire Payouts** | Instant European account-to-account settlement (< 5s) to ING Bank NL43INGB0006372146 | `app/src/main/res/drawable/img_store_payouts_1792326407777.jpg` |
| **4. Client Store Ledger** | Business profiles, tax IDs, contact registry, and live Firestore sync status | `app/src/main/res/drawable/img_store_clients_1792326420777.jpg` |
| **5. Retainers & Subscriptions** | Recurring billing contracts, automated invoice generation, and revenue health | `app/src/main/res/drawable/img_store_retainers_1792326435252.jpg` |

---

## 3. Financial Dashboard & Real-Time Analytics

The Dashboard is your operational command center:
1. **Total Invoiced & Collected Metrics**: Displays lifetime gross revenue, collected amounts, and collection rate percentages.
2. **Cash Flow Visual Health Bar**: A tripartite visual progress gauge breaking down Paid, Pending, and Overdue fractions.
3. **Owner Payout Quick Card**: Instant glance at funds available for owner distribution, beneficiary bank details, and one-tap withdrawal via Wero.
4. **Recent Invoices Carousel**: Horizontally scrollable list of recent transactions with immediate status change actions.
5. **Quick Create Invoice Modal**: Rapid action button to draft an invoice in under 30 seconds.

---

## 4. Digital Invoicing & PDF Export

### Creating an Invoice
1. Tap the **+ New Invoice** button on the Dashboard or Invoices screen.
2. Select a registered client or enter client information directly.
3. Add itemized line items specifying:
   - Description (e.g., "Consulting", "Retainer", "Software License")
   - Unit price and quantity
   - Item-specific discount percentage
   - Applicable VAT rate (default 21% or customizable)
4. Select payment terms (e.g., Due on Receipt, Net 14, Net 30).
5. Tap **Save & Issue Invoice**.

### Managing Invoices & Recording Payments
- **Status Filtering**: Use filter chips to isolate `All`, `Pending`, `Paid`, or `Overdue` invoices.
- **Recording Payments**: When a client pays via bank transfer, iDEAL, credit card, or Stripe, tap **Record Payment**, specify the received amount, method, and transaction reference. If paid in full, the status flips automatically to `Paid`.
- **Exporting to PDF**: Open any invoice detail view and select **Export PDF**. A formatted PDF receipt with company header, itemized breakdown, and ING Bank payment instructions (`NL43 INGB 0006 3721 46`) is rendered and saved to your device.

---

## 5. Client Store Ledger & Cloud Sync

### Client Registry
- Store unlimited client profiles including Legal Company Name, Contact Person, Email Address, Phone Number, Tax/VAT ID, and Postal Address.
- **Client Lifecycle States**:
  - `Good Standing`: All invoices settled on time.
  - `Active`: Regular account activity with current invoices.
  - `Pending Balance`: Invoices awaiting settlement within standard terms.
  - `Overdue`: One or more invoices past their maturity date.
  - `Inactive`: Dormant client accounts.

### Firestore Cloud Synchronization
- Authenticate via Google Sign-In using the top-bar profile icon.
- Tap **Sync to Cloud** to backup your local ledger to Firestore under `/users/{userId}/clients/`.
- All remote modifications propagate back to local Room database automatically.

---

## 6. Bankwire & Wero Instant Payout System

Oistars Billings provides an integrated settlement engine for owner draws and vendor distributions.

### Beneficiary Details
- **Beneficiary Name**: Jurgen Paul Westerveld
- **Entity**: Oistars International B.V.
- **Bank**: ING Bank N.V. (Amsterdam, Netherlands)
- **Official IBAN**: `NL43INGB0006372146` (Formatted: `NL43 INGB 0006 3721 46`)
- **BIC / SWIFT**: `INGBNL2A`
- **Wero / EPI Identifier**: `westerveldjp@gmail.com`

### Supported Transfer Protocols
1. **Wero Instant Wire (European Payments Initiative / EPI)**:
   - **Settlement Speed**: Sub-5-seconds (< 5s) direct account-to-account credit.
   - **Cost**: €0.00 (Zero Fee).
   - **Status**: Live & Recommended.
2. **SEPA Instant Credit Transfer (SCT Inst)**:
   - **Settlement Speed**: Real-time (under 10 seconds), operational 24/7/365.
   - **Cost**: €0.00.
3. **Standard SEPA Bankwire**:
   - **Settlement Speed**: 1–2 European business banking days.

### Automated Payout Schedule
In the **Payouts** tab, configure automated clearance rules:
- **Interval**: Weekly (Every Friday at 17:00 CET), Bi-weekly (15th & Month-End), or Monthly.
- **Minimum Withdrawal Threshold**: Clamps withdrawals below minimum amounts (default €500.00).
- **Safety Reserve Balance**: Maintains a liquid liquidity buffer (default €250.00) in the platform ledger.

---

## 7. Recurring Retainers & Subscriptions

For agency retainers, software licensing, and recurring client services:
1. Navigate to the **Retainers** tab in the bottom navigation.
2. Tap **Add Subscription Plan**.
3. Select the client, billing interval (Monthly, Quarterly, Annually), and contract amount.
4. Enable **Auto-Invoice Generation**: The system automatically provisions a new invoice on the contract renewal date.
5. Use **Bill Now** to instantly generate and issue an off-cycle invoice for the retainer period.

---

## 8. Account Settings & Merchant Configuration

- **Theme Customization**: Toggle between Material 3 Light Mode and Dark Navy Enterprise Theme.
- **Legal Entity Configuration**: Set your Chamber of Commerce (KvK) number, VAT identifier, business street address, and standard tax rates.
- **Biometric App Lock**: Secure local billing data with Android BiometricPrompt (fingerprint or face unlock).
- **Data Safety & Policy Transparency**: Review built-in Data Safety Policy and GDPR compliance declarations directly from the Settings screen.

---

## 9. ProGuard Security & OpenSSF Compliance

Oistars Billings complies with Google Play Store code integrity guidelines and OpenSSF best practices:
- **R8 Full-Mode Obfuscation**: Production APKs/AABs are compressed, with debug symbols stripped and package hierarchies obfuscated.
- **Room Database Hardening**: SQLite database schemas and entity converters are retained without leaking column mappings.
- **Network Security**: Strict HTTPS/TLS enforcement on all external API requests.
- **Source Code Verification**: Comprehensive configuration details are documented in [`PROGUARD_SECURITY.md`](PROGUARD_SECURITY.md) and [`app/proguard-rules.pro`](app/proguard-rules.pro).

---

## 10. Troubleshooting & Support Channels

### Frequently Asked Questions (FAQ)

**Q: Can I use Oistars Billings without internet?**  
A: Yes. All database operations take place in your local on-device SQLite database. Syncing with Firebase Firestore happens automatically once network connectivity is re-established.

**Q: Where can I find generated PDF invoices?**  
A: PDFs are saved to your device's external documents directory and can be shared via email, WhatsApp, or Drive.

**Q: How do I verify my payout went through Wero?**  
A: Tap any completed payout in the **Payouts** tab to open the cryptographic receipt modal. The settlement protocol displays `Wero Instant Wire (EPI)` with the corresponding timestamp and ING reference code.

### Support Contacts
- **Billing Lead**: Jurgen Paul Westerveld (`westerveldjp@gmail.com`)
- **Customer Support**: `oistarsentertainment@gmail.com`
- **Security Disclosures**: `oistarsentertainment@gmail.com`
- **GitHub Repository**: [https://github.com/jpaul8282/Oistars-Billings2](https://github.com/jpaul8282/Oistars-Billings2)
