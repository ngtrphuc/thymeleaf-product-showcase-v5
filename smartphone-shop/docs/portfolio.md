# Smartphone Shop Portfolio Narrative

## Elevator Pitch

Smartphone Shop is a production-style migration project that moved a Spring Boot + Thymeleaf monolith to an API-first architecture with a decoupled Next.js frontend.
The migration preserved business behavior while replacing server-rendered customer/admin flows with modern App Router pages and JWT cookie authentication.

## Problem and Goal

- Original state:
  - tightly coupled MVC templates and backend rendering
  - mixed responsibilities between UI routing and domain logic
- Goal:
  - keep proven business logic in Spring Boot
  - migrate UI delivery to Next.js with minimal production risk
  - harden the stack for deployment and observability

## What Was Delivered

- API-first backend with Spring Boot (`/api/v1/**`) and JWT cookie auth
- Fully migrated Next.js storefront and admin journeys
- Decommissioned legacy Thymeleaf customer/admin controllers and templates
- Redis cache for public catalog/product endpoints with admin-side cache eviction
- CI pipeline via GitHub Actions
- Monitoring stack: Prometheus + Grafana + Alertmanager provisioning in Docker Compose

## Key Technical Decisions

- Incremental migration over rewrite:
  - reduced rollout risk and kept domain logic stable
- `httpOnly` JWT cookie:
  - stronger XSS posture and clean integration with Next.js route protection
- Public-read caching only:
  - avoids leaking user-scoped data (wishlist/cart context)
- SSE/WebSocket-compatible backend preserved during frontend transition:
  - enables real-time support chat without coupling to template runtime

## What Reviewers Can Verify Quickly

1. Start stack with scripts in `scripts/` and open:
   - storefront: `http://localhost:3000`
   - backend docs: `http://localhost:8080/swagger-ui/index.html`
2. Run tests:
   - `./mvnw test`
3. Bring up monitoring profile:
   - `docker compose --profile monitoring up -d prometheus grafana alertmanager`
   - Grafana: `http://localhost:3001` (admin/admin)
   - Dashboard: `Smartphone Shop / Smartphone Shop Overview`

## Suggested Demo Flow (5-7 minutes)

1. Browse products and open detail page.
2. Login/register flow with cookie-auth session.
3. Add to cart and place order.
4. Open admin dashboard, review order updates.
5. Show monitoring dashboard and active API metrics.

## Screenshots and Media

Use the checklist in `docs/screenshots/README.md` to capture portfolio assets.
