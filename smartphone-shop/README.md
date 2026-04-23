# Smartphone Shop

A full-stack smartphone e-commerce platform built with an API-first backend
and a Next.js App Router frontend.

This README documents the project architecture, major feature sets,
technologies, and refactor progress. It also includes a detailed repository
inventory at file level.

## Table of Contents

- [Project Overview](#project-overview)
- [Feature Set](#feature-set)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [API Domain Map](#api-domain-map)
- [Local Development](#local-development)
- [Environment Configuration](#environment-configuration)
- [Refactor Progress](#refactor-progress)
- [Testing and Quality](#testing-and-quality)
- [Repository Structure (Detailed)](#repository-structure-detailed)
- [Contributor Notes](#contributor-notes)

## Project Overview

Smartphone Shop models a production-oriented commerce workflow:

- Browse catalog and product detail pages.
- Manage cart, wishlist, and compare slots.
- Complete checkout with full payment or installment plans.
- Track order lifecycle and customer/admin chat.
- Operate an admin area for dashboard, products, and orders.

## Feature Set

### Storefront Features

- Authentication with JWT in httpOnly cookies.
- Product catalog with search, filter, sorting, and pagination.
- Product detail pages with quick actions.
- Cart and checkout flow.
- Profile and payment methods.
- Order history and cancellation (business-rule based).
- Wishlist and compare.
- Customer support chat.

### Admin Features

- Dashboard overview.
- Product management.
- Order management and status updates.
- Chat conversation management.

### Cross-Cutting Features

- Login and API rate limiting.
- Checkout idempotency.
- Caching for catalog/detail with user-specific overlays.
- Optional Meilisearch with database fallback.
- Metrics, tracing, dashboards, and alerting.

## System Architecture

```mermaid
flowchart LR
    Browser[Customer/Admin Browser] --> Next[Next.js 16 App Router]
    Next -->|REST + Cookie Auth| Api[Spring Boot 3.5 API]

    Api --> Pg[(PostgreSQL)]
    Api --> Redis[(Redis)]
    Api --> Meili[(Meilisearch - optional)]

    Api --> Ws[WebSocket/STOMP + SSE]
    Api --> Actuator[Spring Actuator]

    Actuator --> Prom[Prometheus]
    Prom --> Grafana[Grafana]
    Api --> OTel[OTLP Exporter]
    OTel --> Jaeger[Jaeger]
```

## Technology Stack

### Backend Stack

- Java 21.
- Spring Boot 3.5.13.
- Spring Web, Validation, Security, Data JPA, Data Redis, WebSocket,
  and Actuator.
- Flyway migrations.
- Caffeine + Redis cache.
- JWT via `jjwt` 0.12.6.
- OpenAPI via `springdoc-openapi-starter-webmvc-ui`.
- Micrometer + Prometheus registry + OpenTelemetry bridge/exporter.

### Frontend Stack

- Next.js 16.2.4 (App Router).
- React 19.2.4.
- TypeScript 5.x.
- Tailwind CSS 4.
- Playwright E2E.

### Data and Infrastructure

- PostgreSQL 16.
- Redis 7.
- Meilisearch 1.13.
- Docker Compose for local orchestration.

### Tooling and CI

- Maven Wrapper.
- ESLint for `frontend-next`.
- GitHub Actions workflow in
  `.github/workflows/smartphone-shop-ci.yml`.

## API Domain Map

### Customer APIs (`/api/v1/**`)

- Auth.
- Catalog and product detail.
- Cart.
- Checkout and orders.
- Profile and payment methods.
- Wishlist.
- Compare.
- Chat (REST + SSE).

### Admin APIs (`/api/v1/admin/**`)

- Dashboard.
- Products.
- Orders.
- Chat.

### API Docs

- Swagger UI: `<http://localhost:8080/swagger-ui/index.html>`.

## Local Development

### Prerequisites

- Java 21.
- Node.js 20+.
- Docker and Docker Compose.

### Recommended One-Command Start

Windows PowerShell:

```powershell
./scripts/start-dev-stack.ps1
```

macOS/Linux:

```bash
./scripts/start-dev-stack.sh
```

### Manual Start

- Start infrastructure:

```bash
docker compose up -d postgres redis meilisearch
```

- Run backend:

```bash
./mvnw spring-boot:run
```

- Run frontend:

```bash
cd frontend-next
npm install
npm run dev
```

### Default Endpoints

- Frontend: `<http://localhost:3000>`.
- Backend: `<http://localhost:8080>`.
- Swagger UI: `<http://localhost:8080/swagger-ui/index.html>`.
- Health: `<http://localhost:8080/actuator/health>`.

## Environment Configuration

### Backend Profiles

- Local default: `dev`.
- Production: `SPRING_PROFILES_ACTIVE=prod`.

### Important Backend Variables

- `JWT_SECRET`, `JWT_ACCESS_TOKEN_MINUTES`.
- `APP_CORS_ALLOWED_ORIGINS`.
- `APP_SEARCH_MEILI_ENABLED`, `APP_SEARCH_MEILI_HOST`,
  `APP_SEARCH_MEILI_API_KEY`.
- `DATASOURCE_URL`, `DATASOURCE_USER`, `DATASOURCE_PASSWORD`.
- `REDIS_HOST`, `REDIS_PORT`.
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`.

### Frontend Variables

- `NEXT_PUBLIC_API_BASE_URL` (or compatible API base env wired in
  `frontend-next/src/lib/api.ts`).

## Refactor Progress

### Backend API-First Standardization

Status: stable baseline completed.

- Modularized service/repository/controller layers.
- Security filter chain with JWT and rate limits.
- Idempotent checkout flow and order workflow processing.
- Cache strategy for catalog/product detail.

### Frontend Migration to Next.js

Status: core user journeys completed and actively iterated.

- Storefront routes migrated under App Router.
- Admin routes available in Next.js.
- Shared UI components and storefront modules consolidated.

### Legacy Frontend Footprint

Status: retained for asset compatibility.

- `frontend/static` remains as a legacy asset source.
- Runtime emphasis is on `frontend-next` for active UI.

### Observability and Operations

Status: production-like local baseline available.

- Prometheus, Grafana, and Alertmanager profile in Docker Compose.
- Jaeger tracing path enabled via OTLP.

## Testing and Quality

### Backend Test Commands

```bash
./mvnw test
./mvnw -DskipTests compile
```

### Frontend Test Commands

```bash
cd frontend-next
npm run lint
npm run build
npm run test:e2e
```

## Repository Structure (Detailed)

The tree below is file-level for code/config files.
For image and SVG assets, it intentionally shows only the folder level.

- Scope includes current workspace files.
- Excludes generated/runtime directories: `.next`, `node_modules`, `target`,
  `.data`.
- Excludes local ephemeral files: `.env.local`, Playwright last-run cache.

<!-- markdownlint-disable MD013 -->

```text
smartphone-shop/
├── .github/
│   ├── java-upgrade/
│   │   ├── hooks/
│   │   │   └── scripts/
│   │   │       ├── recordToolUse.ps1
│   │   │       └── recordToolUse.sh
│   │   └── .gitignore
│   └── workflows/
│       └── smartphone-shop-ci.yml
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── .vscode/
│   ├── launch.json
│   └── tasks.json
├── backend/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/
│       │   │       └── github/
│       │   │           └── ngtrphuc/
│       │   │               └── smartphone_shop/
│       │   │                   ├── api/
│       │   │                   │   ├── dto/
│       │   │                   │   │   ├── AuthMeResponse.java
│       │   │                   │   │   ├── AuthTokenResponse.java
│       │   │                   │   │   ├── CartItemResponse.java
│       │   │                   │   │   ├── CartResponse.java
│       │   │                   │   │   ├── CatalogPageResponse.java
│       │   │                   │   │   ├── ChatMessageResponse.java
│       │   │                   │   │   ├── CompareResponse.java
│       │   │                   │   │   ├── ErrorResponse.java
│       │   │                   │   │   ├── OperationStatusResponse.java
│       │   │                   │   │   ├── OrderItemResponse.java
│       │   │                   │   │   ├── OrderResponse.java
│       │   │                   │   │   ├── PaymentMethodResponse.java
│       │   │                   │   │   ├── ProductDetailResponse.java
│       │   │                   │   │   ├── ProductSummary.java
│       │   │                   │   │   ├── ProfileResponse.java
│       │   │                   │   │   ├── WishlistItemResponse.java
│       │   │                   │   │   └── WishlistResponse.java
│       │   │                   │   ├── ApiExceptionHandler.java
│       │   │                   │   └── ApiMapper.java
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
│       │   │                   │   ├── PaymentSimulationProperties.java
│       │   │                   │   ├── ProductSearchProperties.java
│       │   │                   │   ├── SecurityConfig.java
│       │   │                   │   ├── WebConfig.java
│       │   │                   │   └── WebSocketConfig.java
│       │   │                   ├── controller/
│       │   │                   │   ├── api/
│       │   │                   │   │   └── v1/
│       │   │                   │   │       ├── AdminChatApiController.java
│       │   │                   │   │       ├── AdminDashboardApiController.java
│       │   │                   │   │       ├── AdminOrderApiController.java
│       │   │                   │   │       ├── AdminProductApiController.java
│       │   │                   │   │       ├── AuthApiController.java
│       │   │                   │   │       ├── CartApiController.java
│       │   │                   │   │       ├── ChatApiController.java
│       │   │                   │   │       ├── CompareApiController.java
│       │   │                   │   │       ├── OrderApiController.java
│       │   │                   │   │       ├── PaymentMethodApiController.java
│       │   │                   │   │       ├── ProductApiController.java
│       │   │                   │   │       ├── ProfileApiController.java
│       │   │                   │   │       └── WishlistApiController.java
│       │   │                   │   └── RootController.java
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
│       │   │                   │   ├── spec/
│       │   │                   │   │   └── ProductCatalogSpecifications.java
│       │   │                   │   ├── CartItemRepository.java
│       │   │                   │   ├── ChatMessageRepository.java
│       │   │                   │   ├── CompareItemRepository.java
│       │   │                   │   ├── OrderIdempotencyKeyRepository.java
│       │   │                   │   ├── OrderRepository.java
│       │   │                   │   ├── PaymentMethodRepository.java
│       │   │                   │   ├── ProductRepository.java
│       │   │                   │   ├── UserRepository.java
│       │   │                   │   └── WishlistItemRepository.java
│       │   │                   ├── security/
│       │   │                   │   ├── ApiRateLimitFilter.java
│       │   │                   │   ├── ClientIpResolver.java
│       │   │                   │   ├── JwtAuthenticationFilter.java
│       │   │                   │   ├── JwtProperties.java
│       │   │                   │   ├── JwtStompChannelInterceptor.java
│       │   │                   │   ├── JwtTokenProvider.java
│       │   │                   │   └── LoginRateLimitFilter.java
│       │   │                   ├── service/
│       │   │                   │   ├── AuthService.java
│       │   │                   │   ├── CartService.java
│       │   │                   │   ├── ChatService.java
│       │   │                   │   ├── ChatSseRegistry.java
│       │   │                   │   ├── CompareService.java
│       │   │                   │   ├── CustomUserDetailsService.java
│       │   │                   │   ├── OrderIdempotencyService.java
│       │   │                   │   ├── OrderService.java
│       │   │                   │   ├── OrderWorkflowProcessor.java
│       │   │                   │   ├── PaymentMethodService.java
│       │   │                   │   ├── ProductSearchService.java
│       │   │                   │   ├── SimulatedPaymentGateway.java
│       │   │                   │   └── WishlistService.java
│       │   │                   ├── DevFrontendBootstrap.java
│       │   │                   ├── DevInfrastructureBootstrap.java
│       │   │                   ├── Port8080Guard.java
│       │   │                   └── SmartphoneShopApplication.java
│       │   └── resources/
│       │       ├── db/
│       │       │   └── migration/
│       │       │       ├── V1__baseline_schema.sql
│       │       │       ├── V2__performance_indexes.sql
│       │       │       ├── V3__idempotency_and_recommendation_indexes.sql
│       │       │       └── V4__stale_placeholder_cleanup.sql
│       │       ├── application.properties
│       │       ├── application-dev.properties
│       │       └── application-prod.properties
│       └── test/
│           ├── java/
│           │   └── io/
│           │       └── github/
│           │           └── ngtrphuc/
│           │               └── smartphone_shop/
│           │                   ├── common/
│           │                   │   └── support/
│           │                   │       ├── AssetUrlResolverTest.java
│           │                   │       └── CacheKeysTest.java
│           │                   ├── config/
│           │                   │   ├── ApplicationPropertiesDefaultProfileTest.java
│           │                   │   ├── DataInitializerTest.java
│           │                   │   └── PaymentMethodSchemaInitializerTest.java
│           │                   ├── controller/
│           │                   │   ├── api/
│           │                   │   │   └── v1/
│           │                   │   │       ├── AdminApiControllerTest.java
│           │                   │   │       ├── AuthApiControllerTest.java
│           │                   │   │       ├── CartApiControllerTest.java
│           │                   │   │       ├── CompareApiControllerTest.java
│           │                   │   │       ├── OrderApiControllerTest.java
│           │                   │   │       └── ProductApiControllerTest.java
│           │                   │   └── RootControllerTest.java
│           │                   ├── model/
│           │                   │   └── PaymentMethodTest.java
│           │                   ├── repository/
│           │                   │   └── ProductCatalogSpecificationIntegrationTest.java
│           │                   ├── security/
│           │                   │   ├── ApiRateLimitFilterTest.java
│           │                   │   ├── JwtTokenProviderTest.java
│           │                   │   └── LoginRateLimitFilterTest.java
│           │                   ├── service/
│           │                   │   ├── AuthServiceTest.java
│           │                   │   ├── CartServiceTest.java
│           │                   │   ├── ChatServiceTest.java
│           │                   │   ├── CompareServiceTest.java
│           │                   │   ├── MockitoNullSafety.java
│           │                   │   ├── OrderIdempotencyServiceTest.java
│           │                   │   ├── OrderServiceTest.java
│           │                   │   ├── OrderWorkflowProcessorTest.java
│           │                   │   ├── PaymentMethodServiceTest.java
│           │                   │   ├── SimulatedPaymentGatewayTest.java
│           │                   │   └── WishlistServiceTest.java
│           │                   ├── DevFrontendBootstrapTest.java
│           │                   ├── Port8080GuardTest.java
│           │                   └── SmartphoneShopApplicationTests.java
│           └── resources/
│               └── application-test.properties
├── docs/
│   ├── screenshots/
│   │   └── README.md
│   └── portfolio.md
├── frontend/
│   └── static/
│       ├── customer/
│       │   └── images/
│       └── svg/
│           └── griddy/
│               └── README.md
├── frontend-next/
│   ├── public/
│   │   ├── griddy/
│   │   └── payments/
│   ├── src/
│   │   ├── app/
│   │   │   ├── (auth)/
│   │   │   │   ├── login/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── register/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── layout.tsx
│   │   │   ├── (storefront)/
│   │   │   │   ├── cart/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── chat/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── checkout/
│   │   │   │   │   ├── success/
│   │   │   │   │   │   └── page.tsx
│   │   │   │   │   ├── loading.tsx
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── compare/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── orders/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── products/
│   │   │   │   │   ├── [id]/
│   │   │   │   │   │   ├── loading.tsx
│   │   │   │   │   │   ├── not-found.tsx
│   │   │   │   │   │   └── page.tsx
│   │   │   │   │   ├── error.tsx
│   │   │   │   │   ├── loading.tsx
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── profile/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── wishlist/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── layout.tsx
│   │   │   ├── admin/
│   │   │   │   ├── chat/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── orders/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── products/
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── layout.tsx
│   │   │   │   └── page.tsx
│   │   │   ├── asset-proxy/
│   │   │   │   └── [...path]/
│   │   │   │       └── route.ts
│   │   │   ├── globals.css
│   │   │   ├── layout.tsx
│   │   │   └── page.tsx
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
│   │   │   ├── format.ts
│   │   │   └── order-status.ts
│   │   └── proxy.ts
│   ├── tests/
│   │   ├── auth.spec.ts
│   │   └── checkout.spec.ts
│   ├── .env.example
│   ├── .gitignore
│   ├── AGENTS.md
│   ├── CLAUDE.md
│   ├── eslint.config.mjs
│   ├── next.config.ts
│   ├── next-env.d.ts
│   ├── package.json
│   ├── package-lock.json
│   ├── playwright.config.ts
│   ├── postcss.config.mjs
│   ├── README.md
│   └── tsconfig.json
├── monitoring/
│   ├── alertmanager/
│   │   └── alertmanager.yml
│   ├── alerts/
│   │   └── smartphone-shop-alerts.yml
│   ├── grafana/
│   │   └── provisioning/
│   │       ├── dashboards/
│   │       │   ├── json/
│   │       │   │   └── smartphone-shop-overview.json
│   │       │   └── dashboard.yml
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

<!-- markdownlint-enable MD013 -->

## Contributor Notes

- Follow `.editorconfig` conventions.
- Do not commit secrets or local-only credentials.
- Prefer service-layer business logic over controller-level shortcuts.
- Keep API contracts backward compatible unless intentionally versioned.
