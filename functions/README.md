# Oistars Billings - Cloud Functions & Firebase Admin Backend

This directory contains the server-side backend logic for **Oistars Billings**, powered by **Cloud Functions for Firebase** and the **Firebase Admin SDK**.

---

## Architecture & Features

1. **Automated Order Fulfillment (`onOrderCreated`)**
   - Automatically listens to new `orders` written to `/users/{userId}/orders/{orderId}`.
   - Instantly generates an official VAT-compliant `invoices` document in `/users/{userId}/invoices/`.
   - Records an instant payment ledger entry in `/users/{userId}/payments/`.
   - Automatically decrements catalog stock quantities in `/products/{productId}`.

2. **Audit & Order Status Sync (`onOrderUpdated`)**
   - Tracks fulfillment status changes (`PENDING` -> `PAID` -> `COMPLETED` -> `SHIPPED`).
   - Writes immutable timestamped audit entries to `/users/{userId}/audit_logs/`.

3. **REST API & Webhooks (`api`)**
   - `GET /api/health` - Health check and database connection diagnostics.
   - `POST /api/orders` - External e-commerce webhook or API order submission.
   - `GET /api/orders?clientID=...` - List orders for a client.
   - `GET /api/revenue/monthly-trend?months=6` - Aggregated monthly revenue and payment analytics.
   - `POST /api/webhooks/wero` - Real-time settlement webhook for European Payments Initiative (Wero) and SEPA Instant wire transfers.

---

## Configuration & Credentials

### Local Development / Node Scripts
Place your Firebase service account private key at:
```bash
functions/serviceAccountKey.json
```
Or set the environment variable:
```bash
export SERVICE_ACCOUNT_KEY_PATH="/path/to/serviceAccountKey.json"
```

### Production / Google Cloud Platform
On Cloud Functions or Cloud Run, `firebase-admin` automatically leverages **Application Default Credentials (ADC)** without requiring a physical key file.

---

## Available NPM Scripts

```bash
# Seed initial store catalog and demo orders into Firestore
npm run seed

# Run standalone end-to-end order test
npm run test-order

# Run local Firebase Emulator suite
npm run serve

# Deploy Cloud Functions to Firebase
npm run deploy
```
