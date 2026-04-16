# Review Playbook

## Architecture

- Backend code: `backend/src/main/java/io/github/ngtrphuc/smartphone_shop`
- Customer controllers: `controller/user`
- Admin controllers: `controller/admin`
- Business logic: `service`
- Persistence: `repository`
- Entities and DTO-like models: `model`
- Templates: `frontend/templates/customer`, `frontend/templates/admin`
- Static assets: `frontend/static/customer`, `frontend/static/admin`, `frontend/static/js`

## High-Risk Flows

### Checkout and stock

Read these first:
- `controller/user/CartController.java`
- `service/CartService.java`
- `service/OrderService.java`
- `model/Order.java`
- `repository/OrderRepository.java`

Look for:
- stock deduction and rollback behavior
- checkout state stored in `HttpSession`
- GET flows that mutate persistent cart data
- payment detail handling and masking

### Chat and unread counters

Read these first:
- `service/ChatService.java`
- `controller/admin/ChatAdminController.java`
- `controller/user/ChatUserController.java`
- `templates/admin/chat.html`
- `templates/customer/fragments/chat-widget.html`

Look for:
- emitter lifecycle and stale connections
- unread counter consistency
- message validation and escaping
- SSE reconnect assumptions

### Account and payment data

Read these first:
- `service/AuthService.java`
- `service/PaymentMethodService.java`
- `controller/user/ProfileController.java`
- `controller/user/PaymentMethodController.java`
- `templates/customer/profile.html`
- `templates/customer/payment-select.html`

Look for:
- plaintext sensitive data
- default payment method state transitions
- ownership checks on mutable resources

### Catalog and filters

Read these first:
- `controller/user/MainController.java`
- `config/DataInitializer.java`
- `repository/ProductRepository.java`

Look for:
- filter options that do not match seeded data
- in-memory filtering or paging
- brittle brand inference from product names

## Confirmed Hotspots To Re-Verify

These are worth checking before every audit because they are already close to the edge:

1. `templates/customer/my-orders.html`
- Contains mojibake in the quantity separator instead of a proper multiplication marker such as `&times;`.

2. `service/WishlistService.java`
- `getWishlist()` is marked `@Transactional(readOnly = true)` but still calls `deleteAll(...)`.

3. `service/PaymentMethodService.java` plus profile/payment templates
- Bank transfer detail is stored raw and rendered raw in profile and payment selection screens.

4. `controller/user/MainController.java`
- Brand list is hard-coded while seeded products include brands outside the list.
- Any brand, battery, or screen filter pushes catalog filtering into memory.

5. `service/CartService.java`
- `getDbCart()` repairs data by deleting or saving rows during a read path.
- `syncCartCount()` calls that method, so a seemingly read-only request can write to the database.

## Commands

- Full tests: `./mvnw.cmd test`
- Search for transaction mismatches:
  `rg -n "@Transactional\\(readOnly = true\\)|deleteAll\\(|saveAll\\(" backend/src/main/java`
- Search for raw payment detail rendering:
  `rg -n "pm.detail|paymentDetail|bankDetail" backend/src/main/java frontend/templates/customer`
- Search for catalog filter hotspots:
  `rg -n "BRANDS|requiresInMemoryFiltering|findAllWithFilters|inferBrand" backend/src/main/java`

## Review Output Pattern

1. Findings first, ordered by severity.
2. Each finding should include:
- what breaks
- why it breaks
- where it lives
- the safest fix path
3. Add open questions only if they block confidence.
4. Put feature ideas after the review, not before.

