# Smartphone Shop

Smartphone Shop is in **hybrid migration mode**:

- Backend: Spring Boot REST API + legacy Thymeleaf controllers/views
- Frontend mới: Next.js App Router (`frontend-next/`) đã bắt đầu dùng API thật

## Current Tech Stack

### Backend

- Java 21
- Spring Boot 3.5.13
- Spring Web
- Spring Security 6
- Spring Data JPA (Hibernate)
- PostgreSQL
- Flyway migration
- JWT (`jjwt`)
- WebSocket/STOMP
- Springdoc OpenAPI (Swagger)

### Frontend

- Next.js 16 (App Router)
- React 19
- TypeScript 5
- Tailwind CSS v4

### Tooling & Infra

- Maven Wrapper (`mvnw`, `mvnw.cmd`)
- Docker Compose (PostgreSQL + Redis)
- VS Code Tasks + Launch (auto-start infra/full-stack)

## Run (Dev)

### One-click full stack (Windows)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-dev-stack.ps1
```

Script này sẽ:

- tự bật Docker Desktop (nếu chưa mở)
- `docker compose up -d postgres redis`
- mở terminal backend (`mvnw.cmd spring-boot:run`)
- mở terminal frontend (`frontend-next` -> `npm.cmd run dev`)

### Run from VS Code

- Mở **Run and Debug**
- Chọn cấu hình: `Smartphone Shop: Full Stack (Backend + Frontend)`
- Bấm Run

### URLs

- Frontend mới (Next.js): `http://localhost:3000/products`
- Backend API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Legacy Thymeleaf web: `http://localhost:8080/`

> Nếu bạn mở `http://localhost:8080` và thấy JSON `UNAUTHORIZED`, hãy dùng frontend mới ở `http://localhost:3000/products` hoặc truy cập route web legacy cụ thể (`/login`, `/product/{id}`) theo quyền hiện tại.

## Admin Account (Dev)

Mặc định trong `application-dev.properties`:

- Email: `admin@smartphone.local`
- Password seed: `Admin@123456`

Lưu ý:

- Tài khoản admin chỉ được seed nếu DB chưa có dữ liệu user tương ứng.
- Nếu bạn đã chạy DB trước đó, mật khẩu cũ trong DB sẽ được giữ lại.

## Project Structure (Detailed)

> Yêu cầu đặc biệt đã áp dụng: **file hình ảnh/SVG chỉ liệt kê tới thư mục chứa**, không liệt kê từng file ảnh.

```text
smartphone-shop/
├── .github/
│   └── java-upgrade/
│       ├── hooks/
│       │   └── scripts/
│       │       ├── recordToolUse.ps1
│       │       └── recordToolUse.sh
│       └── .gitignore
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
│       │   │                   │   └── exception/
│       │   │                   │       ├── BusinessException.java
│       │   │                   │       ├── ResourceNotFoundException.java
│       │   │                   │       ├── UnauthorizedActionException.java
│       │   │                   │       └── ValidationException.java
│       │   │                   ├── config/
│       │   │                   │   ├── AdminAccountInitializer.java
│       │   │                   │   ├── DataInitializer.java
│       │   │                   │   ├── GlobalModelAttributes.java
│       │   │                   │   ├── LoginSuccessHandler.java
│       │   │                   │   ├── PaymentMethodSchemaInitializer.java
│       │   │                   │   ├── SecurityConfig.java
│       │   │                   │   ├── ThymeleafConfig.java
│       │   │                   │   ├── WebConfig.java
│       │   │                   │   └── WebSocketConfig.java
│       │   │                   ├── controller/
│       │   │                   │   ├── admin/
│       │   │                   │   │   ├── AdminController.java
│       │   │                   │   │   └── ChatAdminController.java
│       │   │                   │   ├── api/
│       │   │                   │   │   └── v1/
│       │   │                   │   │       ├── AuthApiController.java
│       │   │                   │   │       ├── CartApiController.java
│       │   │                   │   │       ├── ChatApiController.java
│       │   │                   │   │       ├── CompareApiController.java
│       │   │                   │   │       ├── OrderApiController.java
│       │   │                   │   │       ├── PaymentMethodApiController.java
│       │   │                   │   │       ├── ProductApiController.java
│       │   │                   │   │       ├── ProfileApiController.java
│       │   │                   │   │       └── WishlistApiController.java
│       │   │                   │   └── user/
│       │   │                   │       ├── AuthController.java
│       │   │                   │       ├── CartController.java
│       │   │                   │       ├── ChatUserController.java
│       │   │                   │       ├── CompareController.java
│       │   │                   │       ├── MainController.java
│       │   │                   │       ├── OrderController.java
│       │   │                   │       ├── PaymentMethodController.java
│       │   │                   │       ├── ProfileController.java
│       │   │                   │       └── WishlistController.java
│       │   │                   ├── event/
│       │   │                   │   └── ChatMessageCreatedEvent.java
│       │   │                   ├── model/
│       │   │                   │   ├── CartItem.java
│       │   │                   │   ├── CartItemEntity.java
│       │   │                   │   ├── ChatMessage.java
│       │   │                   │   ├── CompareItemEntity.java
│       │   │                   │   ├── Order.java
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
│       │   │                   │   ├── OrderRepository.java
│       │   │                   │   ├── PaymentMethodRepository.java
│       │   │                   │   ├── ProductRepository.java
│       │   │                   │   ├── UserRepository.java
│       │   │                   │   └── WishlistItemRepository.java
│       │   │                   ├── security/
│       │   │                   │   ├── JwtAuthenticationFilter.java
│       │   │                   │   ├── JwtProperties.java
│       │   │                   │   ├── JwtStompChannelInterceptor.java
│       │   │                   │   └── JwtTokenProvider.java
│       │   │                   ├── service/
│       │   │                   │   ├── AuthService.java
│       │   │                   │   ├── CartService.java
│       │   │                   │   ├── ChatService.java
│       │   │                   │   ├── ChatWebSocketNotifier.java
│       │   │                   │   ├── CompareService.java
│       │   │                   │   ├── CustomUserDetailsService.java
│       │   │                   │   ├── OrderService.java
│       │   │                   │   ├── OrderValidationException.java
│       │   │                   │   ├── PaymentMethodService.java
│       │   │                   │   └── WishlistService.java
│       │   │                   ├── support/
│       │   │                   │   └── StorefrontSupport.java
│       │   │                   ├── DevInfrastructureBootstrap.java
│       │   │                   ├── Port8080Guard.java
│       │   │                   └── SmartphoneShopApplication.java
│       │   └── resources/
│       │       ├── db/
│       │       │   └── migration/
│       │       │       └── V1__baseline_schema.sql
│       │       ├── application.properties
│       │       ├── application-dev.properties
│       │       └── application-prod.properties
│       └── test/
│           ├── java/
│           │   └── io/
│           │       └── github/
│           │           └── ngtrphuc/
│           │               └── smartphone_shop/
│           │                   ├── config/
│           │                   │   ├── ApplicationPropertiesDefaultProfileTest.java
│           │                   │   └── PaymentMethodSchemaInitializerTest.java
│           │                   ├── controller/
│           │                   │   ├── api/
│           │                   │   │   └── v1/
│           │                   │   │       ├── AuthApiControllerTest.java
│           │                   │   │       └── ProductApiControllerTest.java
│           │                   │   └── user/
│           │                   │       ├── AuthControllerTest.java
│           │                   │       ├── CartControllerTest.java
│           │                   │       ├── CompareControllerTest.java
│           │                   │       ├── MainControllerTest.java
│           │                   │       └── PaymentMethodControllerTest.java
│           │                   ├── model/
│           │                   │   └── PaymentMethodTest.java
│           │                   ├── service/
│           │                   │   ├── AuthServiceTest.java
│           │                   │   ├── CartServiceTest.java
│           │                   │   ├── MockitoNullSafety.java
│           │                   │   ├── OrderServiceTest.java
│           │                   │   ├── PaymentMethodServiceTest.java
│           │                   │   └── WishlistServiceTest.java
│           │                   ├── Port8080GuardTest.java
│           │                   └── SmartphoneShopApplicationTests.java
│           └── resources/
│               └── application-test.properties
├── frontend/
│   ├── static/
│   │   ├── admin/
│   │   │   ├── css/
│   │   │   │   └── style.css
│   │   │   └── js/
│   │   │       └── admin-shell.js
│   │   ├── customer/
│   │   │   ├── css/
│   │   │   │   └── style.css
│   │   │   ├── images/                 # Image assets only (files omitted)
│   │   │   └── js/
│   │   │       ├── auth-password-toggle.js
│   │   │       └── order-success.js
│   │   └── svg/                        # SVG assets only (files omitted)
│   │       └── griddy/
│   │           └── README.md
│   └── templates/
│       ├── admin/
│       │   ├── error/
│       │   │   └── access-denied-admin.html
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
│   ├── public/                         # SVG/image assets only (files omitted)
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
│   │   ├── components/
│   │   │   └── storefront/
│   │   │       └── product-card.tsx
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

## Quick Validation Commands

```powershell
# Backend
.\mvnw.cmd test

# Frontend
cd .\frontend-next
npm.cmd run lint
npm.cmd run build
```
