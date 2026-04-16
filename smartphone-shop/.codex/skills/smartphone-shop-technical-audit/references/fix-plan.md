# Fix Plan

## 1. My Orders mojibake

Files:
- `frontend/templates/customer/my-orders.html`

Fix:
- Replace the broken separator string with `&times;` or plain ASCII `x`.
- Keep the template escaped with `th:text`.

Validation:
- Open the page and confirm order rows render as `Product x 2` or `Product &times; 2`, depending on the chosen output.

## 2. Wishlist write inside read-only transaction

Files:
- `backend/src/main/java/io/github/ngtrphuc/smartphone_shop/service/WishlistService.java`

Fix:
- Remove `readOnly = true` from `getWishlist()`, or split orphan cleanup into a separate write method.
- Prefer cleanup during product deletion or a scheduled maintenance path so ordinary reads stay side-effect free.

Validation:
- Add a service test that verifies orphaned wishlist rows are cleaned up without relying on read-only semantics.

## 3. Raw bank detail exposure

Files:
- `backend/src/main/java/io/github/ngtrphuc/smartphone_shop/service/PaymentMethodService.java`
- `frontend/templates/customer/profile.html`
- `frontend/templates/customer/payment-select.html`

Fix:
- Do not render raw bank detail back to the UI.
- Short term: persist only a masked display value for reusable payment methods.
- Stronger option: encrypt stored detail with an `AttributeConverter` and expose a masked getter for templates.
- Keep `Order.getPaymentMethodDisplayName()` style masking as the display model.

Validation:
- Profile and payment selection screens should show only masked data such as `****7890`.

## 4. Catalog filter mismatch and in-memory scaling

Files:
- `backend/src/main/java/io/github/ngtrphuc/smartphone_shop/controller/user/MainController.java`
- `backend/src/main/java/io/github/ngtrphuc/smartphone_shop/config/DataInitializer.java`
- `backend/src/main/java/io/github/ngtrphuc/smartphone_shop/repository/ProductRepository.java`

Fix:
- Replace the hard-coded customer brand list with a derived list or a normalized brand field on `Product`.
- Push brand and numeric filters down to JPA queries instead of fetching all filtered products into memory.
- If the project will grow, add a dedicated `brand` column and backfill it from the initializer.

Validation:
- Customer catalog exposes all seeded brands.
- Filtering still paginates at the database layer.

## 5. Cart reads that mutate the database

Files:
- `backend/src/main/java/io/github/ngtrphuc/smartphone_shop/service/CartService.java`

Fix:
- Keep `getDbCart()` read-only and move normalization into explicit maintenance or mutation paths.
- If automatic repair is desired, run it when items are added, updated, merged, or during checkout.
- Avoid calling write logic from `syncCartCount()` or other rendering paths.

Validation:
- Viewing the cart or profile should not issue delete/save operations for cart rows.

## Nice-To-Have Hardening

1. CSP hardening
- Move inline scripts and styles into static assets so `unsafe-inline` can be removed.

2. Chat emitter hygiene
- Add heartbeat or periodic pruning if high chat traffic exposes stale SSE emitter buildup.

3. CSS consolidation
- Customer and admin stylesheets are both large; extract shared primitives into a common file once UI churn slows down.

