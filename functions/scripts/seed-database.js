/**
 * Oistars Billings - Database Seeder Script (Firebase Admin SDK)
 * Usage: node scripts/seed-database.js
 */
const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

const serviceAccountPath = process.env.SERVICE_ACCOUNT_KEY_PATH || path.join(__dirname, "..", "serviceAccountKey.json");

if (fs.existsSync(serviceAccountPath)) {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log("Initialized Firebase Admin with serviceAccountKey.json");
} else {
  admin.initializeApp();
  console.log("Initialized Firebase Admin with Application Default Credentials");
}

const DATABASE_ID = process.env.FIRESTORE_DATABASE_ID || "ai-studio-android-oistarsb-ae8638b7-0186-4ab3-a014-3348c77a4205";
let db;
try {
  db = admin.firestore(DATABASE_ID);
} catch (e) {
  db = admin.firestore();
}

async function seed() {
  console.log("Starting backend Firestore seeding...");
  const batch = db.batch();

  // 1. Seed Products Catalog
  const products = [
    {
      id: "PROD-CLOUD-HOST",
      name: "Enterprise Cloud Hosting",
      price: 249.00,
      stockQuantity: 45,
      description: "Dedicated high-performance European cloud node with 99.99% SLA and automated backup.",
      imageUrl: "",
      category: "Infrastructure"
    },
    {
      id: "PROD-TECH-RETAINER",
      name: "Monthly Dev & Maintenance Retainer",
      price: 850.00,
      stockQuantity: 12,
      description: "20 hours of senior Android, Kotlin backend, and API maintenance with priority turnaround.",
      imageUrl: "",
      category: "Services"
    },
    {
      id: "PROD-SECURITY-AUDIT",
      name: "Fintech & OpenSSF Security Audit",
      price: 1450.00,
      stockQuantity: 6,
      description: "Comprehensive ProGuard, zero-trust authentication, and SEPA/EPI compliance audit.",
      imageUrl: "",
      category: "Consulting"
    },
    {
      id: "PROD-API-GATEWAY",
      name: "Billing API Gateway License",
      price: 120.00,
      stockQuantity: 99,
      description: "Annual production API token for webhook settlement triggers and real-time ledger sync.",
      imageUrl: "",
      category: "Software"
    },
    {
      id: "PROD-WERO-INTEGRATION",
      name: "Wero EPI Instant Wire Module",
      price: 399.00,
      stockQuantity: 25,
      description: "Plug-and-play European Payments Initiative (Wero) bank wire SDK with real-time settlement.",
      imageUrl: "",
      category: "Software"
    },
    {
      id: "PROD-VAT-COMPLIANCE",
      name: "EU VAT Reverse-Charge Package",
      price: 180.00,
      stockQuantity: 50,
      description: "Automated cross-border EU tax validation and VIES verification plug-in for invoices.",
      imageUrl: "",
      category: "Services"
    }
  ];

  for (const prod of products) {
    const ref = db.collection("products").doc(prod.id);
    batch.set(ref, prod, { merge: true });
  }

  // 2. Seed Sample Store Orders for default owner
  const userId = "jurgen-westerveld";
  const now = Date.now();
  const dayMs = 24 * 60 * 60 * 1000;

  const sampleOrders = [
    {
      id: "ORD-10001",
      clientID: userId,
      userId: userId,
      productIDs: ["PROD-CLOUD-HOST", "PROD-API-GATEWAY"],
      amount: 369.00,
      status: "PAID",
      timestamp: now - (12 * dayMs),
      customerName: "Nordic Gourmet Brands B.V.",
      customerEmail: "accounting@nordicgourmet.eu",
      shippingAddress: "Keizersgracht 421, 1016 EK Amsterdam"
    },
    {
      id: "ORD-10002",
      clientID: userId,
      userId: userId,
      productIDs: ["PROD-SECURITY-AUDIT"],
      amount: 1450.00,
      status: "PAID",
      timestamp: now - (3 * dayMs),
      customerName: "Artisan Pearl Boutique",
      customerEmail: "billing@artisanpearl.nl",
      shippingAddress: "Witte de Withstraat 88, 3012 BT Rotterdam"
    }
  ];

  for (const order of sampleOrders) {
    const ref = db.collection("users").doc(userId).collection("orders").doc(order.id);
    batch.set(ref, order, { merge: true });
  }

  await batch.commit();
  console.log("Successfully seeded products and orders to Firestore!");
  process.exit(0);
}

seed().catch(err => {
  console.error("Seeding error:", err);
  process.exit(1);
});
