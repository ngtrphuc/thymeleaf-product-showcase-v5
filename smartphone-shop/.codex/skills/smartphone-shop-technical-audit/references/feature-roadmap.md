# Feature Roadmap

## Quick Wins

### 1. Product reviews and ratings

Why:
- Adds social proof and helps conversions.

Where:
- New review entity and repository
- Customer order history gate so only purchasers can review
- Product detail template for review list and review form
- Admin moderation screen if needed later

### 2. Order timeline

Why:
- Makes order status more understandable than a plain badge.

Where:
- Extend `Order` status presentation
- Update `my-orders.html`, `profile.html`, and `admin/orders.html`

### 3. Email notifications

Why:
- Confirms purchase and reduces support load.

Where:
- Trigger on order creation and status changes
- Use Thymeleaf email templates to match current stack

### 4. Coupon or promo codes

Why:
- Simple business lever with clear user value.

Where:
- Checkout session flow
- Order total calculation
- Admin management page for coupon CRUD

## Mid-Sized Features

### 5. Product comparison

Why:
- Smartphone catalogs benefit from side-by-side spec comparison.

Where:
- Customer list/detail pages
- Reuse existing product spec fields already stored on `Product`

### 6. Admin analytics dashboard

Why:
- The current admin dashboard is summary-heavy but not trend-heavy.

Where:
- New aggregation queries on orders
- Chart rendering in `templates/admin/dashboard.html`

### 7. Back-in-stock alerts

Why:
- Re-engages users when inventory returns.

Where:
- Subscription entity keyed by email and product
- Trigger when stock crosses from `0` to `>0`

## Longer-Term

### 8. Smarter product recommendations

Why:
- The product model already stores chipset, OS, storage, battery, and size.

Where:
- Similar-product section on detail page
- Start rule-based before adding anything AI-driven

### 9. AI-assisted shopping chat

Why:
- Builds on the existing chat UI and SSE infrastructure.

Where:
- Route simple product questions to an assistant
- Keep admin takeover available for escalation
