# Smartphone Shop

Smartphone Shop is an e-commerce web application focused on smartphone retail flows.
The project is now API-first with Next.js as the primary UI for both storefront and admin operations.

## Project Overview

This repository demonstrates a full-stack commerce architecture with:

- A Java/Spring backend exposing REST APIs
- A fully migrated Next.js App Router frontend for storefront and admin user journeys
- Redis-backed caching for public product APIs
- Optional Meilisearch-powered relevance/fuzzy product search with DB fallback

## Current Status

The codebase has moved to a decoupled frontend/backend model for customer and admin operations.

- Backend migration progress: ~95-100%
- Next.js migration progress (user-journey coverage): ~95-100%
- Thymeleaf customer flows have been decommissioned
- Thymeleaf admin fallback has been decommissioned
- New UI work is developed in `frontend-next/`
- Backend APIs in `backend/` are the primary contract for Next.js

### Delivery Status

- [x] Migration complete for storefront and admin (Next.js-first)
- [x] Test coverage in place for Auth, Product, Order, Admin APIs
- [x] Redis cache integration and cache invalidation flow
- [x] Production hardening baseline
- [x] CI/CD workflow
- [x] Small test gap closed: expanded `CartApi` scenarios + added `CompareService` and `ChatService` tests
- [x] Monitoring dashboard + alerting provisioning
- [x] Portfolio polish baseline (screenshots checklist + project narrative)

## Progress Snapshot (Updated: 2026-04-23)

### Completed recently

- Ecommerce "wow-level" readiness upgrades:
  - Added global API rate limiting for `/api/v1/**` (not only login) with configurable exclude paths and `Retry-After` support
  - Added asynchronous post-checkout workflow via `OrderCreatedEvent` + `@TransactionalEventListener(AFTER_COMMIT)` + dedicated thread pool executor
  - Added CDN-ready asset URL resolver (`app.assets.base-url`) so product/cart/wishlist image URLs can switch to CDN without frontend code changes
  - Aligned local infra bootstrap to start Meilisearch together with PostgreSQL and Redis in dev scripts/bootstrapping
- Auth foundation for Next.js:
  - JWT cookie (`httpOnly`) login/logout flow
  - Route guarding via Next.js proxy (`frontend-next/src/proxy.ts`)
- Role-separated navigation hardening:
  - Admin sessions default to `/admin` on root access
  - Storefront navigation no longer exposes an `Admin` shortcut for customer sessions
  - Admin header navigation includes `Dashboard`, `Products`, `Orders`, `Chat`, `Home`, and `Logout`
  - Storefront products page removed the `Items / page` selector
- New/expanded backend API coverage:
  - `POST /api/v1/orders` for API-first checkout
  - Admin API namespace under `/api/v1/admin` (dashboard, products, orders, chat)
- Catalog performance optimization:
  - Replaced in-memory batch scan/filter flow in `ProductApiController` with DB-side paging/filtering for brand, battery, and screen-size criteria
  - Preserved existing API response contract while removing full-table scan behavior under advanced filters
  - Aligned `ProductCatalogSpecificationIntegrationTest` with current `ProductCatalogSpecifications.forCatalog(...)` signature by including `storage` argument before `batteryRange` to prevent argument-order compile errors
- Admin product query optimization:
  - Removed in-memory brand filtering from `/api/v1/admin/products`
  - Moved admin brand filtering into repository query with paging + DB sort to avoid full-list scans
- Admin dashboard stability fix:
  - Fixed lazy-loading errors on `/api/v1/admin/dashboard` by loading paged orders with items inside transaction scope
- Chat SSE concurrency hardening:
  - Upgraded user emitter registry to `ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>` to avoid concurrent mutation risks in subscribe/prune/push flows
- JWT production hardening:
  - Added fail-fast startup validation for `prod` profile if JWT secret still uses the default placeholder
  - Kept local `dev/test` startup behavior unchanged
- Login endpoint hardening:
  - Added per-client rate limiting filter for `POST /api/v1/auth/login` (returns `429 TOO_MANY_REQUESTS` when threshold is exceeded)
  - Backed by Caffeine TTL cache (instead of unbounded in-memory map cleanup heuristics)
  - Configurable via `APP_LOGIN_RATE_LIMIT_MAX_ATTEMPTS`, `APP_LOGIN_RATE_LIMIT_WINDOW_SECONDS`, and `APP_LOGIN_RATE_LIMIT_MAX_CLIENTS`
- Cart cleanup optimization:
  - Removed aggressive `cleanupDbCart(...)` calls from item mutation hot-paths (`add/increase/decrease/remove`)
  - Added scheduled cleanup job `cleanupDbCartForAllUsers` (config: `app.cart.cleanup-delay-ms`)
  - Replaced per-user cleanup scans with bulk repository maintenance queries to avoid N+1 scheduler work
- Rate-limit proxy hardening:
  - Added trusted-proxy-aware client IP resolution so `X-Forwarded-For` is only used when the immediate caller matches `APP_TRUSTED_PROXIES`
  - Applied consistently to login rate limiting and global API rate limiting
- Checkout idempotency cleanup hardening:
  - Added scheduled cleanup for stale `order_idempotency_keys` placeholders
  - Added dedicated DB index for pending placeholder cleanup scans
- Admin API modularization:
  - Split the previous monolith into `AdminDashboardApiController`, `AdminProductApiController`, `AdminOrderApiController`, and `AdminChatApiController`
  - Kept `/api/v1/admin/**` contracts stable while reducing controller size and coupling
- Chat SSE maintainability:
  - Extracted emitter lifecycle management into `ChatSseRegistry`
  - `ChatService` now focuses on chat business behavior and repository/event orchestration
- Wishlist hot-path optimization:
  - Removed orphan cleanup calls from `addItem` / `removeItem`
  - Added scheduled cleanup job `cleanupOrphanedItemsForAllUsers` (config: `app.wishlist.cleanup-delay-ms`)
- Cache key stability:
  - Replaced delimiter-based catalog cache keys with SHA-256 hashed keys via `CacheKeys.catalog(...)`
  - Added dedicated hashed key builder for product detail via `CacheKeys.productDetail(...)`
- Product API cache safety:
  - Product catalog/detail now cache public payload for all sessions
  - User-specific `wishlisted` flags are overlaid after cache retrieval to avoid cross-user cache leakage
- JWT cookie deployment safety:
  - Added `app.jwt.cookie.secure` override for explicit secure-cookie policy
  - Default remains dev-friendly (`false`) and prod-safe (`true`)
- Data initialization cleanup:
  - Unified duplicated product key normalization logic in `DataInitializer` (`canonicalProductKey(...)`)
- CORS hardening:
  - Removed unsafe wildcard fallback for empty allowed-origins configuration
  - Backend now fails fast if `app.cors.allowed-origins` resolves to an empty list
- Frontend resiliency improvements:
  - Added retry strategy for safe idempotent API requests in `frontend-next/src/lib/api.ts`
  - Added automatic client-side redirect to `/login` on `401` for protected calls (with opt-out for auth endpoints)
  - Added loading skeleton components for products and checkout flows
- Production-readiness polish:
  - Enabled graceful shutdown (`server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=30s`)
  - Enabled Hikari metrics/MBeans in `dev` and `prod` profiles for Prometheus/Grafana visibility
- New regression coverage:
  - Added Playwright checkout E2E tests (`frontend-next/tests/checkout.spec.ts`)
  - Added Testcontainers integration test for DB-side product catalog filtering (`ProductCatalogSpecificationIntegrationTest`)
- Architecture debt cleanup:
  - `OrderValidationException` moved to `common/exception`
  - `ChatWebSocketNotifier` moved to `infrastructure/websocket`
  - `StorefrontSupport` moved to `common/support`
- Next.js route migration:
  - `products`, `products/[id]`
  - `login`, `register`
  - `cart`, `checkout`, `profile`, `orders`, `wishlist`, `compare`
  - `admin`, `admin/products`, `admin/orders`, `admin/chat`
- CI setup added:
  - GitHub Actions workflow at `.github/workflows/smartphone-shop-ci.yml`
- Build tooling stabilization:
  - Maven source mapping is now IDE-friendly via `build-helper-maven-plugin`
  - Custom source layout `backend/src/main/java` and `backend/src/test/java` is explicitly registered for Maven/Java Language Server
- Decommission milestone:
  - Removed `controller/user/*` customer MVC controllers
  - Removed `frontend/templates/customer/*`
  - Removed `frontend/static/customer/css` and `frontend/static/customer/js`
  - Kept `frontend/static/customer/images` for shared product assets
  - Removed `controller/admin/*` admin MVC controllers
  - Removed `frontend/templates/admin/*` and `frontend/static/admin/*`
  - Removed `ThymeleafConfig` and `GlobalModelAttributes`
  - Simplified `SecurityConfig` to API-first + static image fallback only
  - Removed `LoginSuccessHandler` (legacy form-login customer bridge)
- Test coverage milestone:
  - Added `OrderApiControllerTest` with business-critical auth and validation scenarios
  - Added `AdminApiControllerTest` for role-based access verification (`ROLE_USER` forbidden, `ROLE_ADMIN` allowed)
  - Expanded `CartApiControllerTest` with edge/error/unauthorized cases
  - Added `CompareServiceTest` and `ChatServiceTest` for service-layer behavior
- Production hardening milestone:
  - Enabled Redis cache integration
  - Added cache for public product catalog/detail endpoints
  - Added cache eviction on admin product mutations
  - Hardened `application-prod.properties` (Flyway validation, metrics exposure, Redis settings, SQL/no-stacktrace defaults)
  - Added optional Prometheus + Grafana stack in `docker-compose` (`monitoring` profile)
  - Added Alertmanager + Prometheus alert rules (API 5xx, p95 latency, cache hit ratio, backend availability)
  - Added Grafana auto-provisioned datasource and dashboard (`Smartphone Shop Overview`)

### Validation status

- Backend test suite: passing (`mvnw test`, re-run after decommission)
- Backend compile: passing (`mvnw -DskipTests compile`, verified after Maven source-mapping update)
- Frontend lint: last known passing
- Frontend build: last known passing

## Core Features

- Product catalog and product detail browsing
- User authentication and profile management
- Cart, wishlist, and compare list workflows
- Checkout and order management
- Payment method selection
- Customer and admin real-time chat support
- Admin dashboard and product/order operations

## Architecture

### Backend (`backend/`)

- Spring Boot application with layered architecture (controller/service/repository)
- Spring Security with JWT support for API authentication
- API rate limiting on login and global API traffic (Caffeine window counters)
- Spring Data JPA + Hibernate for persistence
- Flyway for schema migration
- Asynchronous order post-processing via domain events (`OrderCreatedEvent`)
- Meilisearch integration for typo-tolerant/relevance product lookup with DB fallback
- WebSocket/STOMP for real-time messaging
- OpenAPI/Swagger for API documentation
- Custom backend source layout wired through Maven build-helper for IDE consistency

### Legacy Assets (`frontend/`)

- Shared product image assets used by backend `/images/**` mapping (`static/customer/images`)
- No active Thymeleaf template runtime

### Modern Frontend (`frontend-next/`)

- Next.js App Router + React + TypeScript
- API-driven rendering for new storefront pages

## Architecture Diagram

```mermaid
flowchart LR
    U[User Browser] --> N[Next.js App Router]
    N -->|REST + Cookie Auth| B[Spring Boot API]
    B --> P[(PostgreSQL)]
    B --> R[(Redis Cache)]
    B --> S[(Meilisearch)]
    B --> W[WebSocket/STOMP]
    B -. OrderCreatedEvent .-> O[Async Order Workflow]
    A[Admin Browser] --> N
    B --> M[Actuator Metrics]
    M --> PR[Prometheus]
    PR --> G[Grafana]
```

## Technology Stack

- Backend: Java 21, Spring Boot 3, Spring Security, JPA/Hibernate, Caffeine
- Database/Search: PostgreSQL, Meilisearch
- Frontend: Next.js, React, TypeScript, Tailwind CSS
- Tooling: Maven Wrapper, Docker Compose

## Run Locally

### Option A (recommended scripts)

Windows PowerShell:

```powershell
.\scripts\start-dev-stack.ps1
```

macOS/Linux:

```bash
./scripts/start-dev-stack.sh
```

This boots:

- PostgreSQL + Redis + Meilisearch via Docker Compose
- Backend at `http://localhost:8080`
- Next.js frontend at `http://localhost:3000`
- Health checks for both ports before marking startup as ready

Windows script hardening (`scripts/start-dev-stack.ps1`):

- Auto-installs frontend dependencies if `frontend-next/node_modules` is missing
- Detects stale smartphone-shop processes on `:3000` / `:8080` and restarts safely
- Starts frontend first, then backend, to avoid redirect-to-dead-port behavior
- Disables backend auto-start frontend hook when using the stack script (prevents duplicate frontend race)

Opening `http://localhost:8080/` redirects straight to the Next.js storefront at `http://localhost:3000/`. Use API endpoints such as `/swagger-ui/index.html` or `/actuator/health` directly when you want backend-only surfaces.

On Windows local dev, starting Spring Boot also auto-starts the Next.js frontend if `http://localhost:3000` is not already running, so the root redirect does not land on a dead port. Disable that behavior only if needed with `SMARTPHONE_SHOP_DEV_AUTO_START_FRONTEND=false`.

### Option B (manual start)

1. Start infrastructure:

```bash
docker compose up -d postgres redis meilisearch
```

1. Start backend:

```bash
./mvnw spring-boot:run
```

1. Start frontend:

```bash
cd frontend-next
npm install
npm run dev
```

PowerShell note:

- If your shell blocks `npm` scripts (`npm.ps1 cannot be loaded`), use `npm.cmd` instead of `npm`.
- `frontend-next` defaults to `next dev --webpack` for local stability.
- Use `npm.cmd run dev:turbo` only when you explicitly want Turbopack.

### Environment defaults

- Backend profile defaults to `dev` (`spring.profiles.default=dev`)
- Backend CORS default: `http://localhost:3000`
- Frontend API base example is in `frontend-next/.env.example`
- Frontend API retry defaults:
  - `NEXT_PUBLIC_API_RETRY_COUNT=1`
  - `NEXT_PUBLIC_API_RETRY_BASE_DELAY_MS=250`
- Login rate-limit defaults:
  - `APP_LOGIN_RATE_LIMIT_MAX_ATTEMPTS=8`
  - `APP_LOGIN_RATE_LIMIT_WINDOW_SECONDS=60`
  - `APP_LOGIN_RATE_LIMIT_MAX_CLIENTS=50000`
- Global API rate-limit defaults:
  - `APP_API_RATE_LIMIT_ENABLED=true`
  - `APP_API_RATE_LIMIT_MAX_REQUESTS=180`
  - `APP_API_RATE_LIMIT_WINDOW_SECONDS=60`
  - `APP_API_RATE_LIMIT_MAX_CLIENTS=200000`
  - `APP_API_RATE_LIMIT_EXCLUDED_PATHS=/api/v1/auth/login,/api/v1/auth/register,/api/v1/auth/logout,/api/v1/auth/me`
- Trusted proxy defaults:
  - `APP_TRUSTED_PROXIES=` (empty by default, so forwarded headers are ignored for rate limiting)
- Meilisearch defaults:
  - `APP_SEARCH_MEILI_ENABLED=true` (dev profile)
  - `APP_SEARCH_MEILI_HOST=http://localhost:7700`
  - `APP_SEARCH_MEILI_INDEX_NAME=products`
- Order workflow default:
  - `APP_ORDER_WORKFLOW_ENABLED=true`
- CDN-ready asset URL default:
  - `APP_ASSETS_BASE_URL=` (empty = keep backend/local image paths)
- Wishlist orphan cleanup scheduler default:
  - `APP_WISHLIST_CLEANUP_DELAY_MS=300000` (5 minutes)
- Cart cleanup scheduler default:
  - `APP_CART_CLEANUP_DELAY_MS=300000` (5 minutes)
- Idempotency placeholder cleanup scheduler default:
  - `APP_ORDER_IDEMPOTENCY_CLEANUP_DELAY_MS=300000` (5 minutes)
- JWT cookie secure policy:
  - `APP_JWT_COOKIE_SECURE=false` by default (`application.properties`)
  - `APP_JWT_COOKIE_SECURE=true` by default in production profile (`application-prod.properties`)
- Dev bootstrap admin account (unless overridden by env vars):
  - Email: `admin@smartphone.local`
  - Password: `Admin@123456`

### Troubleshooting local startup

- `ERR_CONNECTION_REFUSED` on `http://localhost:3000`:
  - Prefer running .\scripts\start-dev-stack.ps1 (it auto-recovers missing deps and startup order).
  - Ensure frontend is running in `frontend-next` and logs `Ready`.
  - Start with `npm.cmd run dev` on Windows PowerShell.
  - Check port conflicts: `Get-NetTCPConnection -LocalPort 3000 -ErrorAction SilentlyContinue`.
- Next.js guard file naming:
  - This project uses Next.js `16.x`, where `proxy.ts` is the route-guard entrypoint.
  - Guard implementation is at `frontend-next/src/proxy.ts` (not `middleware.ts`).
- Backend reachable but frontend API calls fail:
  - Verify `NEXT_PUBLIC_API_BASE_URL` in `frontend-next/.env.local`.
  - Ensure backend is up on `http://localhost:8080`.

### Monitoring stack (optional)

Start observability services:

```bash
docker compose --profile monitoring up -d prometheus grafana alertmanager
```

Access points:

- Prometheus: `http://localhost:9090`
- Alertmanager: `http://localhost:9093`
- Grafana: `http://localhost:3001` (`admin` / `admin`)
- Dashboard folder: `Smartphone Shop`
- Dashboard name: `Smartphone Shop Overview`

## Project Structure

Folders that mainly contain images or SVG assets are intentionally collapsed at folder level.

```text
smartphone-shop/
├── .github/
│   ├── java-upgrade/
│   │   ├── .gitignore
│   │   └── hooks/
│   │       └── scripts/
│   │           ├── recordToolUse.ps1
│   │           └── recordToolUse.sh
│   └── workflows/
│       └── smartphone-shop-ci.yml
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── backend/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/
│       │   │       └── github/
│       │   │           └── ngtrphuc/
│       │   │               └── smartphone_shop/
│       │   │                   ├── DevFrontendBootstrap.java
│       │   │                   ├── DevInfrastructureBootstrap.java
│       │   │                   ├── Port8080Guard.java
│       │   │                   ├── SmartphoneShopApplication.java
│       │   │                   ├── api/
│       │   │                   │   ├── ApiExceptionHandler.java
│       │   │                   │   ├── ApiMapper.java
│       │   │                   │   └── dto/
│       │   │                   │       ├── AuthMeResponse.java
│       │   │                   │       ├── AuthTokenResponse.java
│       │   │                   │       ├── CartItemResponse.java
│       │   │                   │       ├── CartResponse.java
│       │   │                   │       ├── CatalogPageResponse.java
│       │   │                   │       ├── ChatMessageResponse.java
│       │   │                   │       ├── CompareResponse.java
│       │   │                   │       ├── ErrorResponse.java
│       │   │                   │       ├── OperationStatusResponse.java
│       │   │                   │       ├── OrderItemResponse.java
│       │   │                   │       ├── OrderResponse.java
│       │   │                   │       ├── PaymentMethodResponse.java
│       │   │                   │       ├── ProductDetailResponse.java
│       │   │                   │       ├── ProductSummary.java
│       │   │                   │       ├── ProfileResponse.java
│       │   │                   │       ├── WishlistItemResponse.java
│       │   │                   │       └── WishlistResponse.java
│       │   │                   ├── common/
│       │   │                   │   ├── exception/
│       │   │                   │   │   ├── BusinessException.java
│       │   │                   │   │   ├── OrderValidationException.java
│       │   │                   │   │   ├── ResourceNotFoundException.java
│       │   │                   │   │   ├── UnauthorizedActionException.java
│       │   │                   │   │   └── ValidationException.java
│       │   │                   │   └── support/
│       │   │                   │       ├── AssetUrlResolver.java
│       │   │                   │       ├── CacheKeys.java
│       │   │                   │       └── StorefrontSupport.java
│       │   │                   ├── config/
│       │   │                   │   ├── AdminAccountInitializer.java
│       │   │                   │   ├── AsyncExecutionConfig.java
│       │   │                   │   ├── DataInitializer.java
│       │   │                   │   ├── PaymentMethodSchemaInitializer.java
│       │   │                   │   ├── ProductSearchProperties.java
│       │   │                   │   ├── SecurityConfig.java
│       │   │                   │   ├── WebConfig.java
│       │   │                   │   └── WebSocketConfig.java
│       │   │                   ├── controller/
│       │   │                   │   ├── RootController.java
│       │   │                   │   └── api/
│       │   │                   │       └── v1/
│       │   │                   │           ├── AdminChatApiController.java
│       │   │                   │           ├── AdminDashboardApiController.java
│       │   │                   │           ├── AdminOrderApiController.java
│       │   │                   │           ├── AdminProductApiController.java
│       │   │                   │           ├── AuthApiController.java
│       │   │                   │           ├── CartApiController.java
│       │   │                   │           ├── ChatApiController.java
│       │   │                   │           ├── CompareApiController.java
│       │   │                   │           ├── OrderApiController.java
│       │   │                   │           ├── PaymentMethodApiController.java
│       │   │                   │           ├── ProductApiController.java
│       │   │                   │           ├── ProfileApiController.java
│       │   │                   │           └── WishlistApiController.java
│       │   │                   ├── event/
│       │   │                   │   ├── ChatMessageCreatedEvent.java
│       │   │                   │   └── OrderCreatedEvent.java
│       │   │                   ├── infrastructure/
│       │   │                   │   └── websocket/
│       │   │                   │       └── ChatWebSocketNotifier.java
│       │   │                   ├── model/
│       │   │                   │   ├── CartItem.java
│       │   │                   │   ├── CartItemEntity.java
│       │   │                   │   ├── ChatMessage.java
│       │   │                   │   ├── CompareItemEntity.java
│       │   │                   │   ├── Order.java
│       │   │                   │   ├── OrderIdempotencyKey.java
│       │   │                   │   ├── OrderItem.java
│       │   │                   │   ├── PaymentMethod.java
│       │   │                   │   ├── Product.java
│       │   │                   │   ├── User.java
│       │   │                   │   ├── WishlistItem.java
│       │   │                   │   └── WishlistItemEntity.java
│       │   │                   ├── repository/
│       │   │                   │   ├── CartItemRepository.java
│       │   │                   │   ├── ChatMessageRepository.java
│       │   │                   │   ├── CompareItemRepository.java
│       │   │                   │   ├── OrderIdempotencyKeyRepository.java
│       │   │                   │   ├── OrderRepository.java
│       │   │                   │   ├── PaymentMethodRepository.java
│       │   │                   │   ├── ProductRepository.java
│       │   │                   │   ├── UserRepository.java
│       │   │                   │   ├── WishlistItemRepository.java
│       │   │                   │   └── spec/
│       │   │                   │       └── ProductCatalogSpecifications.java
│       │   │                   ├── security/
│       │   │                   │   ├── ApiRateLimitFilter.java
│       │   │                   │   ├── ClientIpResolver.java
│       │   │                   │   ├── JwtAuthenticationFilter.java
│       │   │                   │   ├── JwtProperties.java
│       │   │                   │   ├── JwtStompChannelInterceptor.java
│       │   │                   │   ├── JwtTokenProvider.java
│       │   │                   │   └── LoginRateLimitFilter.java
│       │   │                   └── service/
│       │   │                       ├── AuthService.java
│       │   │                       ├── CartService.java
│       │   │                       ├── ChatService.java
│       │   │                       ├── ChatSseRegistry.java
│       │   │                       ├── CompareService.java
│       │   │                       ├── CustomUserDetailsService.java
│       │   │                       ├── OrderIdempotencyService.java
│       │   │                       ├── OrderService.java
│       │   │                       ├── OrderWorkflowProcessor.java
│       │   │                       ├── PaymentMethodService.java
│       │   │                       ├── ProductSearchService.java
│       │   │                       └── WishlistService.java
│       │   └── resources/
│       │       ├── application-dev.properties
│       │       ├── application-prod.properties
│       │       ├── application.properties
│       │       └── db/
│       │           └── migration/
│       │               ├── V1__baseline_schema.sql
│       │               ├── V2__performance_indexes.sql
│       │               ├── V3__idempotency_and_recommendation_indexes.sql
│       │               └── V4__stale_placeholder_cleanup.sql
│       └── test/
│           ├── java/
│           │   └── io/
│           │       └── github/
│           │           └── ngtrphuc/
│           │               └── smartphone_shop/
│           │                   ├── DevFrontendBootstrapTest.java
│           │                   ├── Port8080GuardTest.java
│           │                   ├── SmartphoneShopApplicationTests.java
│           │                   ├── common/
│           │                   │   └── support/
│           │                   │       ├── AssetUrlResolverTest.java
│           │                   │       └── CacheKeysTest.java
│           │                   ├── config/
│           │                   │   ├── ApplicationPropertiesDefaultProfileTest.java
│           │                   │   ├── DataInitializerTest.java
│           │                   │   └── PaymentMethodSchemaInitializerTest.java
│           │                   ├── controller/
│           │                   │   ├── RootControllerTest.java
│           │                   │   └── api/
│           │                   │       └── v1/
│           │                   │           ├── AdminApiControllerTest.java
│           │                   │           ├── AuthApiControllerTest.java
│           │                   │           ├── CartApiControllerTest.java
│           │                   │           ├── CompareApiControllerTest.java
│           │                   │           ├── OrderApiControllerTest.java
│           │                   │           └── ProductApiControllerTest.java
│           │                   ├── model/
│           │                   │   └── PaymentMethodTest.java
│           │                   ├── repository/
│           │                   │   └── ProductCatalogSpecificationIntegrationTest.java
│           │                   ├── security/
│           │                   │   ├── ApiRateLimitFilterTest.java
│           │                   │   ├── JwtTokenProviderTest.java
│           │                   │   └── LoginRateLimitFilterTest.java
│           │                   └── service/
│           │                       ├── AuthServiceTest.java
│           │                       ├── CartServiceTest.java
│           │                       ├── ChatServiceTest.java
│           │                       ├── CompareServiceTest.java
│           │                       ├── MockitoNullSafety.java
│           │                       ├── OrderIdempotencyServiceTest.java
│           │                       ├── OrderServiceTest.java
│           │                       ├── PaymentMethodServiceTest.java
│           │                       └── WishlistServiceTest.java
│           └── resources/
│               └── application-test.properties
├── docs/
│   ├── portfolio.md
│   └── screenshots/
│       └── README.md
├── frontend/
│   ├── static/
│   │   ├── customer/
│   │   │   └── images/
│   │   └── svg/
│   │       └── griddy/
│   └── templates/
├── frontend-next/
│   ├── .env.example
│   ├── .gitignore
│   ├── AGENTS.md
│   ├── CLAUDE.md
│   ├── README.md
│   ├── eslint.config.mjs
│   ├── next-env.d.ts
│   ├── next.config.ts
│   ├── package-lock.json
│   ├── package.json
│   ├── playwright.config.ts
│   ├── postcss.config.mjs
│   ├── public/
│   │   ├── griddy/
│   │   └── payments/
│   ├── src/
│   │   ├── app/
│   │   │   ├── favicon.ico
│   │   │   ├── globals.css
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx
│   │   │   ├── (auth)/
│   │   │   │   ├── layout.tsx
│   │   │   │   ├── login/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── register/
│   │   │   │       └── page.tsx
│   │   │   ├── (storefront)/
│   │   │   │   ├── layout.tsx
│   │   │   │   ├── cart/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── chat/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── checkout/
│   │   │   │   │   ├── loading.tsx
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── compare/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── orders/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── products/
│   │   │   │   │   ├── error.tsx
│   │   │   │   │   ├── loading.tsx
│   │   │   │   │   ├── page.tsx
│   │   │   │   │   └── [id]/
│   │   │   │   │       ├── loading.tsx
│   │   │   │   │       ├── not-found.tsx
│   │   │   │   │       └── page.tsx
│   │   │   │   ├── profile/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── wishlist/
│   │   │   │       └── page.tsx
│   │   │   ├── admin/
│   │   │   │   ├── layout.tsx
│   │   │   │   ├── page.tsx
│   │   │   │   ├── chat/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── orders/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── products/
│   │   │   │       └── page.tsx
│   │   │   └── asset-proxy/
│   │   │       └── [...path]/
│   │   │           └── route.ts
│   │   ├── components/
│   │   │   ├── admin/
│   │   │   │   ├── admin-header-nav.tsx
│   │   │   │   └── admin-session-actions.tsx
│   │   │   ├── auth/
│   │   │   │   └── password-field.tsx
│   │   │   ├── storefront/
│   │   │   │   ├── catalog-filters.tsx
│   │   │   │   ├── catalog-paged-grid.tsx
│   │   │   │   ├── catalog-viewport-sync.tsx
│   │   │   │   ├── checkout-skeleton.tsx
│   │   │   │   ├── filter-dropdown.tsx
│   │   │   │   ├── payment-method-badge.tsx
│   │   │   │   ├── product-actions.tsx
│   │   │   │   ├── product-card.tsx
│   │   │   │   ├── product-detail-skeleton.tsx
│   │   │   │   ├── product-grid-skeleton.tsx
│   │   │   │   ├── quick-product-actions.tsx
│   │   │   │   ├── storefront-chat-bubble.tsx
│   │   │   │   ├── storefront-compare-banner.tsx
│   │   │   │   └── storefront-header-dock-nav.tsx
│   │   │   └── ui/
│   │   │       ├── dock.tsx
│   │   │       ├── expanding-nav.tsx
│   │   │       ├── griddy-icon.tsx
│   │   │       ├── skeleton.tsx
│   │   │       └── vercel-tabs.tsx
│   │   ├── lib/
│   │   │   ├── api.ts
│   │   │   └── format.ts
│   │   └── proxy.ts
│   ├── tests/
│   │   ├── auth.spec.ts
│   │   └── checkout.spec.ts
│   └── tsconfig.json
├── monitoring/
│   ├── alertmanager/
│   │   └── alertmanager.yml
│   ├── alerts/
│   │   └── smartphone-shop-alerts.yml
│   ├── grafana/
│   │   └── provisioning/
│   │       ├── dashboards/
│   │       │   ├── dashboard.yml
│   │       │   └── json/
│   │       │       └── smartphone-shop-overview.json
│   │       └── datasources/
│   │           └── prometheus.yml
│   └── prometheus.yml
├── scripts/
│   ├── start-dev-infra.ps1
│   ├── start-dev-stack.ps1
│   ├── start-dev-stack.sh
│   └── start-frontend-dev.ps1
├── .editorconfig
├── .gitattributes
├── .gitignore
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

Local/generated artifacts intentionally excluded from source control:

- `.data/`
- `target/`
- `frontend-next/.next/`
- `frontend-next/node_modules/`
- `*.log`

## Quality and Validation

- Backend tests are located under `backend/src/test`
- Frontend quality checks (lint/build) are managed inside `frontend-next/`
- Latest local validation snapshot:
  - `mvnw -Dtest=ProductCatalogSpecificationIntegrationTest test`: build success (test is skipped automatically when Docker/Testcontainers is unavailable)
  - `mvnw -Dtest=ApiRateLimitFilterTest,LoginRateLimitFilterTest,OrderServiceTest,AssetUrlResolverTest,ProductApiControllerTest,AuthApiControllerTest test`: passing
  - `mvnw test`: passing
  - `cd frontend-next && npm run lint`: passing (1 existing non-blocking hook warning in `compare/page.tsx`)
  - `cd frontend-next && npm run test:e2e`: passing (2 checkout flow tests)

## Design Decisions

- API-first migration over full rewrite:
  - Reduced delivery risk while keeping backend business logic stable.
  - Allowed incremental rollout of Next.js without blocking feature work.
- `httpOnly` JWT cookie auth:
  - Better XSS resistance than storing tokens in browser-managed storage.
  - Works well with server-side and route-guarded flows in Next.js.
- Keep legacy customer assets only (`frontend/static/customer/images`):
  - Preserves product image compatibility for backend `/images/**` mapping.
  - Removes template/CSS/JS debt while avoiding asset migration churn.
- Cache public product payload + overlay user context:
  - Keeps cache hit-rate high for both anonymous and authenticated traffic.
  - Prevents user-specific response leakage by computing wishlist flags after cache retrieval.
  - Uses explicit cache eviction on admin product writes.
- Scheduled cart/wishlist cleanup over hot-path cleanup:
  - Reduces repeated read/write overhead on every cart or wishlist mutation.
  - Keeps data hygiene via bounded periodic maintenance jobs.

## Optimization Roadmap (Q2 2026)

### P0 (high priority)

- [x] Move cart cleanup from mutation hot-path to scheduled cleanup job.
- [x] Replace unsafe delimiter-based cache keys with hashed keys.
- [x] Replace login rate-limit map cleanup heuristic with Caffeine TTL cache.
- [x] Add explicit JWT cookie secure override (`APP_JWT_COOKIE_SECURE`).
- [x] Add global API rate limiting baseline for `/api/v1/**` traffic.
- [x] Add async order post-processing pipeline (event-driven after checkout commit).
- [x] Add CDN-ready image URL rewrite strategy (`app.assets.base-url`).
- [ ] Add Flyway migration for persistent `order_code` column/index if DB-side search on order code is required.

### P1 (medium priority)

- [ ] Extract and cache brand facet list independently from catalog page payload.
- [ ] Move admin chat from polling to SSE stream path end-to-end in frontend.
- [ ] Add default fetch timeout via `AbortController` in frontend API client.
- [ ] Review authenticated catalog caching strategy to keep high hit-rate while isolating user-specific fields.

### P2 (low priority)

- [ ] Clarify unsupported payment types strategy (remove from enum or explicitly deprecate).
- [ ] Review index/query plan for order history at larger datasets.
- [ ] Expand Playwright scenarios for admin order/product regressions.

## Portfolio Guide

- Technical narrative: `docs/portfolio.md`
- Screenshot checklist: `docs/screenshots/README.md`
- Monitoring dashboard: `monitoring/grafana/provisioning/dashboards/json/smartphone-shop-overview.json`
- Alert rules: `monitoring/alerts/smartphone-shop-alerts.yml`

## Optional Next Iterations

1. Expand Playwright coverage to admin order/product flows.
1. Add Grafana contact points and notification channels (Slack/Email) for real alert delivery.
1. Add deployment automation docs for a cloud target (Render/Fly.io/Azure/GCP).
