# Smartphone Shop

Smartphone Shop is an e-commerce web application focused on smartphone retail flows.
The project is currently in a hybrid migration phase, combining a mature Spring Boot + Thymeleaf implementation with a new Next.js frontend.

## Project Overview

This repository demonstrates a full-stack commerce architecture with:

- A Java/Spring backend exposing both server-rendered pages and REST APIs
- A legacy Thymeleaf UI for existing production-ready customer/admin flows
- A modern Next.js App Router frontend that is being incrementally migrated to API-first rendering

## Current Status

The codebase is actively evolving from monolithic server-rendered pages toward a decoupled frontend/backend model.

- Legacy UI remains stable in `frontend/` (Thymeleaf templates + static assets)
- New UI work is developed in `frontend-next/`
- Backend APIs in `backend/` serve both frontends during migration

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
- Spring Data JPA + Hibernate for persistence
- Flyway for schema migration
- WebSocket/STOMP for real-time messaging
- OpenAPI/Swagger for API documentation

### Legacy Frontend (`frontend/`)

- Thymeleaf server-side templates
- Static CSS/JS assets for customer and admin areas

### Modern Frontend (`frontend-next/`)

- Next.js App Router + React + TypeScript
- API-driven rendering for new storefront pages

## Technology Stack

- Backend: Java 21, Spring Boot 3, Spring Security, JPA/Hibernate
- Database: PostgreSQL
- Frontend: Next.js, React, TypeScript, Tailwind CSS
- Tooling: Maven Wrapper, Docker Compose

## Project Structure

```text
smartphone-shop/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── backend/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/github/ngtrphuc/smartphone_shop/
│       │   │       ├── api/
│       │   │       │   ├── dto/
│       │   │       │   │   ├── AuthMeResponse.java
│       │   │       │   │   ├── AuthTokenResponse.java
│       │   │       │   │   ├── CartItemResponse.java
│       │   │       │   │   ├── CartResponse.java
│       │   │       │   │   ├── CatalogPageResponse.java
│       │   │       │   │   ├── ChatMessageResponse.java
│       │   │       │   │   ├── CompareResponse.java
│       │   │       │   │   ├── ErrorResponse.java
│       │   │       │   │   ├── OperationStatusResponse.java
│       │   │       │   │   ├── OrderItemResponse.java
│       │   │       │   │   ├── OrderResponse.java
│       │   │       │   │   ├── PaymentMethodResponse.java
│       │   │       │   │   ├── ProductDetailResponse.java
│       │   │       │   │   ├── ProductSummary.java
│       │   │       │   │   ├── ProfileResponse.java
│       │   │       │   │   ├── WishlistItemResponse.java
│       │   │       │   │   └── WishlistResponse.java
│       │   │       │   ├── ApiExceptionHandler.java
│       │   │       │   └── ApiMapper.java
│       │   │       ├── common/exception/
│       │   │       │   ├── BusinessException.java
│       │   │       │   ├── ResourceNotFoundException.java
│       │   │       │   ├── UnauthorizedActionException.java
│       │   │       │   └── ValidationException.java
│       │   │       ├── config/
│       │   │       │   ├── AdminAccountInitializer.java
│       │   │       │   ├── DataInitializer.java
│       │   │       │   ├── GlobalModelAttributes.java
│       │   │       │   ├── LoginSuccessHandler.java
│       │   │       │   ├── PaymentMethodSchemaInitializer.java
│       │   │       │   ├── SecurityConfig.java
│       │   │       │   ├── ThymeleafConfig.java
│       │   │       │   ├── WebConfig.java
│       │   │       │   └── WebSocketConfig.java
│       │   │       ├── controller/
│       │   │       │   ├── admin/
│       │   │       │   │   ├── AdminController.java
│       │   │       │   │   └── ChatAdminController.java
│       │   │       │   ├── api/v1/
│       │   │       │   │   ├── AuthApiController.java
│       │   │       │   │   ├── CartApiController.java
│       │   │       │   │   ├── ChatApiController.java
│       │   │       │   │   ├── CompareApiController.java
│       │   │       │   │   ├── OrderApiController.java
│       │   │       │   │   ├── PaymentMethodApiController.java
│       │   │       │   │   ├── ProductApiController.java
│       │   │       │   │   ├── ProfileApiController.java
│       │   │       │   │   └── WishlistApiController.java
│       │   │       │   └── user/
│       │   │       │       ├── AuthController.java
│       │   │       │       ├── CartController.java
│       │   │       │       ├── ChatUserController.java
│       │   │       │       ├── CompareController.java
│       │   │       │       ├── MainController.java
│       │   │       │       ├── OrderController.java
│       │   │       │       ├── PaymentMethodController.java
│       │   │       │       ├── ProfileController.java
│       │   │       │       └── WishlistController.java
│       │   │       ├── event/
│       │   │       │   └── ChatMessageCreatedEvent.java
│       │   │       ├── model/
│       │   │       │   ├── CartItem.java
│       │   │       │   ├── CartItemEntity.java
│       │   │       │   ├── ChatMessage.java
│       │   │       │   ├── CompareItemEntity.java
│       │   │       │   ├── Order.java
│       │   │       │   ├── OrderItem.java
│       │   │       │   ├── PaymentMethod.java
│       │   │       │   ├── Product.java
│       │   │       │   ├── User.java
│       │   │       │   ├── WishlistItem.java
│       │   │       │   └── WishlistItemEntity.java
│       │   │       ├── repository/
│       │   │       │   ├── CartItemRepository.java
│       │   │       │   ├── ChatMessageRepository.java
│       │   │       │   ├── CompareItemRepository.java
│       │   │       │   ├── OrderRepository.java
│       │   │       │   ├── PaymentMethodRepository.java
│       │   │       │   ├── ProductRepository.java
│       │   │       │   ├── UserRepository.java
│       │   │       │   └── WishlistItemRepository.java
│       │   │       ├── security/
│       │   │       │   ├── JwtAuthenticationFilter.java
│       │   │       │   ├── JwtProperties.java
│       │   │       │   ├── JwtStompChannelInterceptor.java
│       │   │       │   └── JwtTokenProvider.java
│       │   │       ├── service/
│       │   │       │   ├── AuthService.java
│       │   │       │   ├── CartService.java
│       │   │       │   ├── ChatService.java
│       │   │       │   ├── ChatWebSocketNotifier.java
│       │   │       │   ├── CompareService.java
│       │   │       │   ├── CustomUserDetailsService.java
│       │   │       │   ├── OrderService.java
│       │   │       │   ├── OrderValidationException.java
│       │   │       │   ├── PaymentMethodService.java
│       │   │       │   └── WishlistService.java
│       │   │       ├── support/
│       │   │       │   └── StorefrontSupport.java
│       │   │       ├── DevInfrastructureBootstrap.java
│       │   │       ├── Port8080Guard.java
│       │   │       └── SmartphoneShopApplication.java
│       │   └── resources/
│       │       ├── db/migration/
│       │       │   └── V1__baseline_schema.sql
│       │       ├── application.properties
│       │       ├── application-dev.properties
│       │       └── application-prod.properties
│       └── test/
│           ├── java/io/github/ngtrphuc/smartphone_shop/
│           │   ├── config/
│           │   │   ├── ApplicationPropertiesDefaultProfileTest.java
│           │   │   └── PaymentMethodSchemaInitializerTest.java
│           │   ├── controller/
│           │   │   ├── api/v1/
│           │   │   │   ├── AuthApiControllerTest.java
│           │   │   │   └── ProductApiControllerTest.java
│           │   │   └── user/
│           │   │       ├── AuthControllerTest.java
│           │   │       ├── CartControllerTest.java
│           │   │       ├── CompareControllerTest.java
│           │   │       ├── MainControllerTest.java
│           │   │       └── PaymentMethodControllerTest.java
│           │   ├── model/
│           │   │   └── PaymentMethodTest.java
│           │   ├── service/
│           │   │   ├── AuthServiceTest.java
│           │   │   ├── CartServiceTest.java
│           │   │   ├── MockitoNullSafety.java
│           │   │   ├── OrderServiceTest.java
│           │   │   ├── PaymentMethodServiceTest.java
│           │   │   └── WishlistServiceTest.java
│           │   ├── Port8080GuardTest.java
│           │   └── SmartphoneShopApplicationTests.java
│           └── resources/
│               └── application-test.properties
├── frontend/
│   ├── static/
│   │   ├── admin/
│   │   │   ├── css/style.css
│   │   │   └── js/admin-shell.js
│   │   ├── customer/
│   │   │   ├── css/style.css
│   │   │   ├── images/ (assets only)
│   │   │   └── js/
│   │   │       ├── auth-password-toggle.js
│   │   │       └── order-success.js
│   │   └── svg/
│   │       └── griddy/README.md
│   └── templates/
│       ├── admin/
│       │   ├── error/access-denied-admin.html
│       │   ├── chat.html
│       │   ├── dashboard.html
│       │   ├── orders.html
│       │   ├── product-form.html
│       │   └── products.html
│       └── customer/
│           ├── auth/
│           │   ├── login.html
│           │   └── register.html
│           ├── fragments/
│           │   ├── chat-widget.html
│           │   ├── compare-bar.html
│           │   └── footer.html
│           ├── cart.html
│           ├── checkout.html
│           ├── compare.html
│           ├── detail.html
│           ├── index.html
│           ├── my-orders.html
│           ├── payment-select.html
│           ├── profile.html
│           ├── shipping.html
│           ├── success.html
│           └── wishlist.html
├── frontend-next/
│   ├── public/ (SVG/image assets)
│   ├── src/
│   │   ├── app/
│   │   │   ├── products/
│   │   │   │   ├── [id]/
│   │   │   │   │   ├── loading.tsx
│   │   │   │   │   ├── not-found.tsx
│   │   │   │   │   └── page.tsx
│   │   │   │   ├── error.tsx
│   │   │   │   ├── loading.tsx
│   │   │   │   └── page.tsx
│   │   │   ├── globals.css
│   │   │   ├── layout.tsx
│   │   │   └── page.tsx
│   │   ├── components/storefront/
│   │   │   └── product-card.tsx
│   │   └── lib/
│   │       ├── api.ts
│   │       └── format.ts
│   ├── .env.example
│   ├── .gitignore
│   ├── AGENTS.md
│   ├── CLAUDE.md
│   ├── eslint.config.mjs
│   ├── next-env.d.ts
│   ├── next.config.ts
│   ├── package-lock.json
│   ├── package.json
│   ├── postcss.config.mjs
│   ├── README.md
│   └── tsconfig.json
├── scripts/
│   ├── start-dev-infra.ps1
│   ├── start-dev-stack.ps1
│   └── start-dev-stack.sh
├── .editorconfig
├── .gitattributes
├── .gitignore
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Quality and Validation

- Backend tests are located under `backend/src/test`
- Frontend quality checks (lint/build) are managed inside `frontend-next/`

## Roadmap

- Continue migrating customer-facing pages to Next.js
- Keep backend APIs backward-compatible during transition
- Expand automated test coverage across critical commerce flows
