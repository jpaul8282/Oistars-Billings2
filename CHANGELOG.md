# Changelog

All notable changes to **Oistars Billings** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [5.0.0] - 2026-10-23

### Added
- **Interactive RevenueChart**: Native Jetpack Compose Canvas area/line chart with cubic bezier smoothing, gradient fills, and touch scrubbers.
- **Real-Time Cloud Firestore Sync**: Added `OrderRepository.listenForRealtimeOrderUpdates` with `DocumentChange` processing for instant order additions and status transitions.
- **Room SQLite Local Persistence**: Added `OrderDao`, `Order` entity, and `StringListConverter` for zero-latency offline-first caching.
- **Backend Cloud Functions & Admin SDK**: Added `/functions` service with automated order fulfillment (`onOrderCreated`), inventory decrementing, and Wero / SEPA Instant webhook handling.
- **Live Real-time Indicator**: Integrated active sync badges and animated order alert banners on the dashboard.

### Changed
- Upgraded `versionCode` to `5` and `versionName` to `5.0` in `app/build.gradle.kts`.
- Hardened ProGuard / R8 minification configurations and enabled strict zero dynamic code loading safeguards.
- Pre-warmed incremental release bundle compilation for sub-second build execution.

### Security
- Verified compliance with OpenSSF Best Practices passing criteria.
- Enforced strict user-scoped security rules in Cloud Firestore (`firestore.rules`).

---

## [4.0.0] - 2026-10-20

### Added
- **Bankwire & Wero Instant Wire Engine**: Real-time European account-to-account settlement to ING Bank N.V. (`NL43INGB0006372146`).
- **Owner Payout Distribution**: Instant and standard SEPA clearing schedules with automated threshold reserves.
- **E-Commerce Storefront**: Product catalog and cart checkout workflow.

---

## [3.0.0] - 2026-10-15

### Added
- **Recurring Retainers & Subscriptions**: Automated subscription billing engine with monthly, quarterly, and annual intervals.
- **Client Store Ledger**: Comprehensive customer directory with Dutch VAT / KVK verification.

---

## [2.0.0] - 2026-10-05

### Added
- **Digital Invoicing**: Multi-status invoice lifecycle management (Draft, Pending, Paid, Overdue).
- **PDF Invoice Export**: Client-side PDF generator with itemized VAT breakdown and payment QR codes.

---

## [1.0.0] - 2026-09-20

### Added
- Initial release of Oistars Billings financial platform.
- Local SQLite database layer using Android Jetpack Room.
- Material Design 3 adaptive dashboard layout.
