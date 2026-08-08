# E-Commerce Shop with Unzer Payments — Architecture

## 1. Overview & Assumptions

This document describes the architecture of an e-commerce backend integrating the Unzer payment platform. It covers customer accounts, catalog, cart, inventory, checkout, orders, and payments. The implementation is a **modular monolith** in Java 17 / Spring Boot; the code deliverable is a working vertical slice (checkout → payment → order confirmation) for Credit Card and one redirect method (Wero/Open Banking), against the Unzer sandbox.

**Assumptions:** PostgreSQL is the system of record; monetary values are stored as integer minor units + ISO 4217 currency; Unzer webhooks are the authoritative payment status source (not the browser redirect); raw card data never reaches our backend (Unzer UI Components/tokenization); secrets are env vars locally and AWS Secrets Manager in production; the frontend is a minimal checkout page, not a full storefront.

## 2. System Decomposition

Seven modules, each owning its own data, communicating through application services (not shared tables):

| Module | Owns | Responsibility |
|---|---|---|
| Customer | Customer, Address | Auth, roles (customer/admin), addresses |
| Catalog | Product, Variant | Browsing, search, admin CRUD |
| Cart | Cart, CartItem | Add/update/remove items |
| Inventory | Inventory, Reservation | Stock levels, reserve/confirm/release |
| Order | Order, OrderItem | Order lifecycle |
| Payment | Payment, Refund, ProcessedWebhook | Unzer integration, webhooks, refunds |
| Checkout | *(none — orchestrator only)* | Coordinates Order + Inventory + Payment |

**Why a modular monolith, not microservices:** at this scope, microservices add distributed-transaction and deployment overhead without payoff. Module boundaries are enforced at the code level (package-private internals, service interfaces as the only cross-module contract), so Catalog, Inventory, or Payment can be extracted into standalone services later if their load profile diverges — Catalog reads scale independently of Checkout writes, for example.

Key contracts: `InventoryService.reserve/confirm/release(orderId, items)`, `OrderService.createOrder/markPaid/markPaymentFailed`, `PaymentService.startPayment/processWebhook`.

```mermaid
flowchart LR
    Customer[Customer] -->|browse, checkout| Shop
    Admin[Shop Admin] -->|manage catalog/orders| Shop
    Shop["E-Commerce Shop<br/>Catalog · Cart · Checkout · Inventory · Orders · Payments"]
    Shop -->|create resources/transactions| Unzer[Unzer Payment Platform]
    Unzer -->|webhooks| Shop
```

```mermaid
flowchart LR
    Customer & Admin --> Web["Checkout UI (HTML/JS)"]
    Web -->|HTTPS/JSON| API["Spring Boot App<br/>(modular monolith)"]
    API -->|JPA/JDBC| DB[("PostgreSQL")]
    API -->|publish| Queue["Amazon SQS<br/>(reconciliation, expiry)"]
    Queue -->|consume| API
    API -->|resources/transactions| Unzer["Unzer API"]
    Unzer -->|webhooks| API
    API -->|read| Secrets["AWS Secrets Manager"]
```

## 3. Domain & Data Model

Money is stored as `(amount_minor: bigint, currency: char(3))` to avoid float rounding. Inventory is tracked per **Variant**: `available_quantity`, `reserved_quantity`. Checkout creates an `InventoryReservation` (`ACTIVE → CONFIRMED | RELEASED | EXPIRED`) instead of deducting stock immediately. `OrderItem` snapshots product name/SKU/price at purchase time so historical orders are immutable. An `Order` can have multiple `Payment` attempts (retry after failure), each carrying an idempotency key, the Unzer payment-type resource ID, and the Unzer transaction ID. `ProcessedWebhook` stores webhook IDs for dedup.

Indexes: `Variant.sku` (unique), `Inventory.variant_id`, `Order.customer_id`, `Payment.order_id`, `Payment.unzer_transaction_id` (unique), `ProcessedWebhook.webhook_id` (unique).

The diagram below shows the primary entity relationships across Customer, Catalog, Cart, Order, and Payment:

```mermaid
erDiagram
    CUSTOMER ||--o{ ADDRESS : owns
    CUSTOMER o|--o{ ORDER : places
    CUSTOMER o|--o{ CART : owns
    PRODUCT ||--|{ VARIANT : has
    VARIANT ||--|| INVENTORY : tracks
    CART ||--o{ CART_ITEM : contains
    VARIANT ||--o{ CART_ITEM : selected_as
    ORDER ||--|{ ORDER_ITEM : contains
    VARIANT ||--o{ ORDER_ITEM : purchased_as
    ORDER ||--o{ INVENTORY_RESERVATION : reserves
    ORDER ||--o{ PAYMENT : has_attempts
    PAYMENT ||--o{ REFUND : refunds
    PAYMENT ||--o{ PROCESSED_WEBHOOK : receives
```

The Order entity moves through the following lifecycle as it's driven by checkout, payment, and fulfillment events:

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> AWAITING_PAYMENT
    AWAITING_PAYMENT --> PAID
    AWAITING_PAYMENT --> PAYMENT_FAILED
    AWAITING_PAYMENT --> CANCELLED : reservation expired
    PAID --> FULFILLING
    FULFILLING --> SHIPPED
    SHIPPED --> COMPLETED
    PAID --> REFUNDED
    COMPLETED --> REFUNDED
    PAYMENT_FAILED --> [*]
    CANCELLED --> [*]
    COMPLETED --> [*]
```

**Why PostgreSQL, single database:** ACID transactions and row-level locking are exactly what order/inventory/payment consistency needs; a single schema (module-partitioned via naming/ownership, not physical separation) keeps the reservation-UPDATE and order-creation atomic where required, while module boundaries stay logical, enforced in code, not by network calls.

## 4. Checkout & Payment Flow

```mermaid
sequenceDiagram
    actor Customer
    participant Checkout
    participant Inventory
    participant Order
    participant Payment
    participant Unzer

    Customer->>Checkout: Submit checkout
    Checkout->>Inventory: Reserve stock
    alt out of stock
        Inventory-->>Checkout: failed
        Checkout-->>Customer: out of stock
    else reserved
        Checkout->>Order: create (AWAITING_PAYMENT)
        Checkout->>Payment: start payment
        Payment->>Unzer: create payment-type resource
        alt Credit Card
            Payment->>Unzer: authorize/charge
        else Wero / Open Banking
            Payment->>Unzer: charge
            Unzer-->>Customer: redirectUrl
            Customer->>Unzer: completes payment
            Unzer-->>Customer: returnUrl (informational only)
        end
        Unzer-->>Payment: webhook (authoritative)
        alt success
            Payment->>Order: markPaid
            Payment->>Inventory: confirm reservation
        else failure
            Payment->>Order: markPaymentFailed
            Payment->>Inventory: release reservation
        end
    end
```

Unzer specifics are hidden behind `PaymentProvider { startPayment(), refund() }`; Checkout/Order depend only on this interface, so adding a 4th method means one new implementation, not a Checkout change. The redirect `returnUrl` only renders a "processing" state — it never mutates Order/Payment. Only the webhook does, applied idempotently via the `ProcessedWebhook` table, so it's safe whether it arrives before or after the customer's browser returns.

## 5. Consistency & Failure Handling

**Overselling** is prevented with a single conditional update, avoiding both application-level locks and lost-update races:
```sql
UPDATE inventory
SET available_quantity = available_quantity - :qty, reserved_quantity = reserved_quantity + :qty
WHERE variant_id = :variantId AND available_quantity >= :qty;
```
Zero rows affected ⇒ insufficient stock, reservation rejected. Multi-item orders reserve all lines in one transaction (all-or-nothing).

**Idempotency:** payment creation and retries are keyed by an idempotency key (same key ⇒ same Payment record returned, no duplicate charge); webhook processing, inventory confirm/release, and refunds all check-then-act against a unique key before mutating state.

**Transaction boundaries:** local state (order + reservation + payment-attempt row) is committed *before* calling Unzer, so we never hold a DB transaction open across a network call. The Unzer response is persisted in a follow-up transaction — this avoids lock contention and means a crash mid-call leaves a safely-retryable "pending" record rather than a stuck lock.

**Reservation expiry:** a scheduled job flips `ACTIVE` reservations past their TTL to `EXPIRED` and returns stock — but only if still `ACTIVE`; a reservation already `CONFIRMED` by a webhook can't be expired, closing the race between "payment just succeeded" and "reservation about to time out."

**Failure walkthroughs:**
- *Payment succeeds, order-update fails:* Unzer identifiers are already persisted; a reconciliation job or the (idempotent) webhook retry re-applies the `PAID` transition. No second charge is ever issued for this.
- *Webhook before redirect:* order is already `PAID` when the customer's browser returns; return page just reads current state.
- *Unzer times out mid-charge:* treated as **unknown**, not failed — payment stays `PENDING`; we poll/reconcile using the stored resource ID rather than blindly re-charging.

Async work (reservation expiry, reconciliation polling, retries) runs via **Amazon SQS** with a DLQ for poison messages, keeping these off the request path.

## 6. Technology Choices (Java)

Spring Boot 3 / Java 17, Spring Data JPA + PostgreSQL, Flyway for migrations. For Unzer, we use the **official Java SDK** rather than raw HTTP: it gives typed resource/transaction objects and reduces boilerplate for the create-resource → authorize/charge pattern shared by all three methods; the webhook *receiver*, however, is a plain Spring `@RestController` endpoint, since webhooks are inbound HTTP regardless of SDK.

Representative code — the trickiest part is idempotent webhook application:

```java
public void handleWebhook(WebhookEvent event) {
    // Atomic dedup: the unique constraint on webhook_id is the real guard —
    // not the (racy) existsById-then-save pattern. Two concurrent deliveries
    // of the same webhook cause the second insert to violate the constraint.
    try {
        processedWebhookRepo.saveAndFlush(new ProcessedWebhook(event.getId()));
    } catch (DataIntegrityViolationException alreadyProcessed) {
        return;
    }
    applyPaymentResult(event);
}

@Transactional
public void applyPaymentResult(WebhookEvent event) {
    Payment payment = paymentRepo.findByUnzerTransactionId(event.getTransactionId())
        .orElseThrow();
    if (payment.getStatus().isTerminal()) return; // already applied

    if (event.isSuccess()) {
        payment.markSucceeded();
        orderService.markPaid(payment.getOrderId());
        inventoryService.confirm(payment.getOrderId());
    } else {
        payment.markFailed();
        orderService.markPaymentFailed(payment.getOrderId());
        inventoryService.release(payment.getOrderId());
    }
}
```
The dedup insert commits in its own step so the unique constraint — not an application-level check — is what actually stops concurrent/duplicate webhook deliveries from double-applying a transition; `applyPaymentResult` then runs in a separate transaction, safe to retry if it fails partway.

## 7. Deployment & AWS

```mermaid
flowchart TB
    CF["CloudFront + S3<br/>(checkout UI)"] --> ALB["Application Load Balancer"]
    ALB --> ECS["ECS Fargate<br/>Spring Boot tasks (autoscaled)"]
    ECS --> RDS[("RDS PostgreSQL<br/>Multi-AZ")]
    ECS --> SQS["SQS + DLQ"]
    ECS --> SM["Secrets Manager"]
    ECS --> CW["CloudWatch<br/>Logs / Metrics / Alarms"]
    Unzer["Unzer API"] <--> ALB
```

- **Compute:** ECS Fargate behind an ALB; stateless app tasks scale horizontally on CPU/request-count. Read-heavy catalog traffic and write-heavy checkout traffic scale the same service today, but the module boundaries mean Catalog could move to its own Fargate service (with a read replica) without touching Checkout/Payment if that path becomes the bottleneck.
- **Data:** RDS PostgreSQL Multi-AZ; read replica optional for catalog/reporting.
- **CI/CD:** GitHub Actions — build → unit tests → integration tests (Testcontainers Postgres + Unzer sandbox for the checkout/webhook flow) → Docker image → push to ECR → Flyway migration → ECS rolling deploy.
- **Secrets:** Unzer API key and DB credentials in Secrets Manager, injected as task env vars; never in source or logs.
- **Observability:** structured logs with correlation ID propagated through checkout → Unzer call → webhook → async worker (CloudWatch Logs); metrics on payment success/failure rate and SQS queue depth (CloudWatch Metrics/Alarms); an alarm on payments stuck `PENDING` beyond a threshold triggers reconciliation review.

## 8. Security

Card data never touches the backend — collected via Unzer UI Components/tokenization (PCI SAQ-A scope). Customer vs. admin roles enforced via Spring Security; admin endpoints require elevated auth. All traffic over HTTPS. Webhooks are signature-verified before processing and deduplicated via `ProcessedWebhook`. Secrets live only in env vars/Secrets Manager. Payment credentials are never logged.

## 9. Trade-offs & Next Steps

Chosen: modular monolith over microservices (simplicity now, extractable later), single Postgres instance over per-module databases (transactional consistency across Order/Inventory), webhook-authoritative over redirect-authoritative payment confirmation (correctness over latency). Left out: full admin UI, real production deployment, distributed tracing, event sourcing/CQRS.

Future enhancements include:

- Extract Payment and Inventory into independently scalable services
- Add PostgreSQL read replicas for catalog workloads
- Add contract tests against the Unzer sandbox in CI
- Introduce distributed tracing (e.g., OpenTelemetry)