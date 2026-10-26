# Security Review: Cart Checkout Concurrency

**Scope:** `OrderServiceimpl.selectProductFromCart` and its directly related cart, product, and order persistence operations.

## Findings

| # | Severity | File | Lines | Vulnerability | Confidence |
|---|----------|------|-------|---------------|------------|
| 1 | 🟡 MEDIUM | `src/main/java/com/example/demo/Service/OrderServiceimpl.java` | 76-88, 114 | Checkout reads the cart and stock, then inserts an order without atomically reserving/decrementing stock or claiming the cart items. Concurrent requests can both pass the stock check and create payable orders for the same remaining inventory. A retry can also create another order because the selected cart rows remain available and there is no idempotency key. | 9/10 |

## Recommended remediation

- In one database transaction, reserve stock with a conditional atomic update (for example, decrement only where `stock >= quantity`) and treat an affected-row count of zero as a failed checkout.
- Add durable checkout idempotency, enforced by a unique database constraint.
- Atomically claim or consume the selected cart rows, and verify they have not changed since checkout began.
- Roll back stock reservation, cart state changes, and order creation together if any step fails.

## Test gaps

The existing tests do not cover concurrent checkout, repeated submission, competing purchases for the last unit, concurrent cart edits, or rollback after a partial checkout failure. At minimum, test that only one checkout succeeds when stock is one, a retry does not create a duplicate order, and a failed checkout leaves stock, cart, and orders consistent.

## Review limitations

The project directory is not inside a Git working tree, so the reviewed code could not be compared against a change diff. The report is based on the implementation present in the workspace.
