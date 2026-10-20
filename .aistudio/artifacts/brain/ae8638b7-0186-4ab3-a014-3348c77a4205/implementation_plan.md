# Monthly Revenue Trend Chart & E-Commerce Order Integration Plan

Deliver a responsive, interactive native Jetpack Compose monthly revenue trend line chart on the dashboard, alongside a full Cloud Firestore-backed e-commerce system (User, Product, Cart, CartItem, Order, OrderItem) that automatically generates invoices and records revenue into the billing and owner payout ledger.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Based on your clarifications, the following architectural choices have been locked in for this implementation:

- **Confirmed Decision 1 (Chart Visualization)**: Render the monthly revenue and payment trend line chart directly on the Android dashboard using a native Jetpack Compose interactive Canvas with cubic bezier curves, gradient fills, month datapoint nodes, and drag/tap touch scrubbers showing exact billing figures.
- **Confirmed Decision 2 (Data Persistence)**: Store User, Product, Cart, and Order records in Cloud Firestore with real-time snapshot listeners and offline-first Room local caching.
- **Confirmed Decision 3 (Billing Ecosystem Connection)**: Automatically generate formal invoices and book recognized revenue into the client ledger and owner payout balance whenever an order is submitted or marked completed.

---

### 1. Overview & Core Concept

- **What It Does**:
  1. **Dashboard Revenue & Payment Growth Analytics**: Visualizes 6-12 month historical billing and received payments on a sleek, interactive line chart with dynamic trend metrics, month-over-month growth percentage badges, and point inspection.
  2. **Product Catalog & Inventory**: Displays products with prices, stock counters, categories, and item imagery.
  3. **Cart & Seamless Checkout**: Enables adding products to a persistent user cart, quantity adjustments, order summary calculations, and instant checkout.
  4. **Order History & Auto-Invoicing**: Orders transition to "Completed", creating a linked invoice in the billing ledger and incrementing available owner payout balances (with instant bank wire via Wero / SEPA Instant).
- **Target Audience / Persona**: Store owners, merchant operators, and finance administrators who need real-time sales visibility, seamless product order handling, and instant payout reconciliation.
- **Key Value**: Bridges customer-facing order checkout with administrative billing operations in a unified, synchronized experience without manual double-entry.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Revenue Growth Exploration**: User opens the Dashboard tab and observes the new "Monthly Revenue Growth" card. They drag across months (Jan–Dec) to inspect billing volume, collected revenue, and growth rates (+18.4% MoM).
  2. **Product Catalog & Cart Management**: User navigates to the "Store / Products" section, filters by category, inspects stock levels, and adds items to the cart with instant bottom-sheet / FAB badge feedback.
  3. **Order Placement & Auto-Invoice Generation**: User checks out their cart, reviews shipping address and order total, and confirms order. The system stores the order in Firestore, creates an order receipt, automatically generates an official Invoice under the customer's profile, and updates the owner balance.
  4. **Payout Settlement**: The store owner can immediately view the newly collected revenue in the "Owner Payouts" screen and initiate instant settlement to ING Bank N.V. via Wero Instant Wire.

- **Visual Identity & Theme**:
  - **Aesthetic Direction**: High-trust Financial & Commerce design. Modern dark/light posture with refined emerald and deep slate tokens.
  - **Color Palette**:
    - Primary: Emerald 600 (`#059669`) / Emerald 400 (`#34D399`) for positive revenue growth and actions.
    - Secondary Accent: Royal Indigo / Blue (`#3B82F6`) for invoice and order tags.
    - Neutral Background: Deep Charcoal / Pure Light surfaces with subtle borders (`1.dp` outline).
    - Trend Line Gradients: Dual-tone cubic bezier curves with vertical gradient area fill (`alpha = 0.25` to `0.02`).
  - **Typography & Hierarchy**:
    - Display Metric: Monospace-styled clean numerical weights for currency amounts (€14,850.00).
    - Labels: Medium 12sp uppercase tracked labels for monthly axes and badges.

- **Interactive Feedback & Motion**:
  - **Chart Touch Scrubber**: Haptic touch feedback when snapping to monthly nodes; floating inspection pill showing "Billing: €X | Payments: €Y".
  - **Cart Micro-interactions**: Quantity stepper bounce animation, animated cart badge counter, and smooth checkout transition.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Jetpack Compose Chart vs Web/WebView**
  - *Chosen Approach*: Pure Jetpack Compose Canvas with smooth cubic bezier curve interpolation, dynamic min/max normalization, and gesture pointer input.
  - *Why*: Eliminates WebView overhead, prevents latency and APK bloat, works offline, and perfectly adheres to Material Design 3 dynamic theme tokens and device pixel density.
  - *Alternatives Considered*: Embedded WebView with Recharts/D3 was considered from the web prompt context, but dismissed in favor of native Android Compose for fluid 60fps gesture scrolling and native touch target compliance.

- **Decision 2: Cloud Firestore with Local Room Cache**
  - *Chosen Approach*: Cloud Firestore repository for `products`, `orders`, and user `cart` with offline persistence enabled, coupled with local Room database tables for instant startup caching.
  - *Why*: Provides multi-device real-time sync while ensuring complete operational resilience if the network drops during order taking or payout checking.

- **Decision 3: Synchronous Invoice & Revenue Generation on Order Completion**
  - *Chosen Approach*: When an order status transitions to `COMPLETED` or `PAID`, the ViewModel writes the order record and invokes the Billing repository to create a matching `InvoiceEntity` and `PaymentEntity`.
  - *Why*: Unifies the billing analytics and dashboard revenue charts with the new e-commerce orders, ensuring charts immediately reflect newly placed orders.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├──────────────────┬──────────────────────┬──────────────────────────────┤
│ DashboardScreen  │ ProductsCatalogView  │ Cart & CheckoutBottomSheet   │
│  - RevenueChart  │  - Product Grid      │  - Cart Item Steppers        │
│  - Growth Badges │  - Stock Status      │  - Submit Order Action       │
└────────┬─────────┴──────────┬───────────┴──────────────┬───────────────┘
         │                    │                          │
         ▼                    ▼                          ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   BillingViewModel & StateFlows                        │
│  - monthlyTrendState: StateFlow<List<MonthlyRevenuePoint>>             │
│  - productsState / cartState / ordersState                             │
│  - placeOrderAndGenerateInvoice()                                      │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │
         ┌────────────────────────┴────────────────────────┐
         ▼                                                 ▼
┌─────────────────────────────────┐       ┌──────────────────────────────┐
│     Cloud Firestore Backend     │       │     Local Room Database      │
│  - /users/{userId}/cart         │       │  - ProductEntity             │
│  - /users/{userId}/orders       │       │  - OrderEntity / OrderItem   │
│  - /products/{productId}        │       │  - InvoiceEntity / Payment   │
│  - Hardened firestore.rules     │       │  - PayoutEntity (Wero/ING)   │
└─────────────────────────────────┘       └──────────────────────────────┘
```

#### Data Entities (Kotlin & Firestore Models):

1. **User**: `id: String`, `email: String`, `fullName: String`, `shippingAddress: String`
2. **Product**: `id: String`, `name: String`, `price: Double`, `stockQuantity: Int`, `description: String`, `imageUrl: String`, `category: String`
3. **CartItem**: `productId: String`, `productName: String`, `quantity: Int`, `priceAtTime: Double`
4. **Order**: `id: String`, `userId: String`, `totalAmount: Double`, `status: String` (PENDING, PAID, SHIPPED), `createdAt: Long`, `items: List<OrderItem>`, `invoiceId: String?`
5. **MonthlyRevenuePoint**: `month: String`, `year: Int`, `invoicedAmount: Double`, `collectedAmount: Double`, `growthRate: Double`

---

### 5. Step-by-Step Execution Phases

1. **Phase 1: Native Jetpack Compose Monthly Revenue Line Chart**
   - Create `MonthlyRevenueTrendChart.kt` featuring smooth cubic bezier curves, month markers, touch scrubber with floating values, and MoM trend stats.
   - Embed the chart prominently in `DashboardScreen.kt` above the recent invoices list.

2. **Phase 2: Product, Cart, and Order Data Layer**
   - Define data models (`Product`, `CartItem`, `Order`, `OrderItem`) in `data/model/CommerceModels.kt`.
   - Update `firestore.rules` and `firebase-blueprint.json` to secure `/products` and `/users/{userId}/orders` paths.
   - Add Firestore commerce repository with real-time snapshot listeners.

3. **Phase 3: Catalog & Cart Checkout UI**
   - Build `ProductsScreen.kt` (product browsing, category chips, stock status, add to cart).
   - Build `CartCheckoutDialog.kt` (item quantity management, shipping address input, instant checkout).

4. **Phase 4: Billing & Owner Payout Ledger Integration**
   - Wire `BillingViewModel` so checking out an order automatically registers the order, logs a matching paid invoice, updates total revenue, and updates the owner balance for Wero instant bank wire withdrawal.

5. **Phase 5: Verification & Build Check**
   - Execute `compile_applet` to confirm clean compilation and zero lint regressions.
