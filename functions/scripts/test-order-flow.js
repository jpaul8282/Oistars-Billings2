/**
 * Test order placement and invoice generation
 * Usage: node scripts/test-order-flow.js
 */
const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

const serviceAccountPath = process.env.SERVICE_ACCOUNT_KEY_PATH || path.join(__dirname, "..", "serviceAccountKey.json");

if (fs.existsSync(serviceAccountPath)) {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
} else {
  admin.initializeApp();
}

const DATABASE_ID = process.env.FIRESTORE_DATABASE_ID || "ai-studio-android-oistarsb-ae8638b7-0186-4ab3-a014-3348c77a4205";
let db;
try {
  db = admin.firestore(DATABASE_ID);
} catch (e) {
  db = admin.firestore();
}

async function run() {
  const userId = "jurgen-westerveld";
  const orderId = `ORD-TEST-${Date.now().toString().slice(-4)}`;
  console.log(`Creating test order ${orderId} in Firestore...`);

  const order = {
    id: orderId,
    clientID: userId,
    productIDs: ["PROD-API-GATEWAY"],
    amount: 120.00,
    status: "PAID",
    timestamp: Date.now(),
    customerName: "Automated QA Test Client",
    customerEmail: "qa@oistars.eu",
    shippingAddress: "Singel 200, 1016 AB Amsterdam"
  };

  await db.collection("users").doc(userId).collection("orders").doc(orderId).set(order);
  console.log(`Order ${orderId} placed successfully in Firestore!`);

  const doc = await db.collection("users").doc(userId).collection("orders").doc(orderId).get();
  console.log("Verified written document data:", doc.data());
  process.exit(0);
}

run().catch(err => {
  console.error("Test order flow failed:", err);
  process.exit(1);
});
