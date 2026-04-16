# Smartphone Shop

A smartphone e-commerce web application built with Spring Boot + Thymeleaf, including both customer and admin flows.

## Key Features

- Sign up/sign in with `ROLE_USER` and `ROLE_ADMIN` authorization
- Product listing with filtering/search and product detail pages
- Product compare flow (customer)
- Shopping cart and multi-step checkout
- Payment method management:
  - `Cash on Delivery`
  - `Bank Transfer`
  - `PayPay`
- Save default shipping address in profile and reuse it during checkout
- Customer order tracking
- Admin management for products, orders, and chat
- Real-time chat between customer and admin (SSE)

## Tech Stack

- Java 21
- Spring Boot 3.5.13
- Spring Security 6
- Spring Data JPA (Hibernate)
- Thymeleaf
- Maven
- H2 (dev/test), MySQL/MariaDB (prod)

## Detailed Project Structure

```text
📂 smartphone-shop
├── 📂 .codex/
│   └── 📂 skills/
│       └── 📂 smartphone-shop-technical-audit/
│           ├── 📂 agents/
│           │   └── 📄 openai.yaml
│           ├── 📂 references/
│           │   ├── 📄 feature-roadmap.md
│           │   ├── 📄 fix-plan.md
│           │   ├── 📄 review-playbook.md
│           │   └── 📄 skill-boundaries.md
│           └── 📄 SKILL.md
├── 📂 .data/
│   ├── 📄 smartphone_shop_dev.lock.db
│   ├── 📄 smartphone_shop_dev.mv.db
│   └── 📄 smartphone_shop_dev.trace.db
├── 📂 .mvn/
│   └── 📂 wrapper/
│       └── 📄 maven-wrapper.properties
├── 📂 .settings/
│   ├── 📄 org.eclipse.core.resources.prefs
│   ├── 📄 org.eclipse.jdt.apt.core.prefs
│   ├── 📄 org.eclipse.jdt.core.prefs
│   └── 📄 org.eclipse.m2e.core.prefs
├── 📂 .vscode/
│   ├── 📄 launch.json
│   └── 📄 settings.json
├── 📂 backend/
│   └── 📂 src/
│       ├── 📂 main/
│       │   ├── 📂 java/io/github/ngtrphuc/smartphone_shop/
│       │   │   ├── 📂 config/
│       │   │   │   ├── 📄 AdminAccountInitializer.java
│       │   │   │   ├── 📄 DataInitializer.java
│       │   │   │   ├── 📄 GlobalModelAttributes.java
│       │   │   │   ├── 📄 LoginSuccessHandler.java
│       │   │   │   ├── 📄 PaymentMethodSchemaInitializer.java
│       │   │   │   ├── 📄 SecurityConfig.java
│       │   │   │   ├── 📄 ThymeleafConfig.java
│       │   │   │   └── 📄 WebConfig.java
│       │   │   ├── 📂 controller/
│       │   │   │   ├── 📂 admin/
│       │   │   │   │   ├── 📄 AdminController.java
│       │   │   │   │   └── 📄 ChatAdminController.java
│       │   │   │   └── 📂 user/
│       │   │   │       ├── 📄 AuthController.java
│       │   │   │       ├── 📄 CartController.java
│       │   │   │       ├── 📄 ChatUserController.java
│       │   │   │       ├── 📄 CompareController.java
│       │   │   │       ├── 📄 MainController.java
│       │   │   │       ├── 📄 OrderController.java
│       │   │   │       ├── 📄 PaymentMethodController.java
│       │   │   │       ├── 📄 ProfileController.java
│       │   │   │       └── 📄 WishlistController.java
│       │   │   ├── 📂 model/
│       │   │   │   ├── 📄 CartItem.java
│       │   │   │   ├── 📄 CartItemEntity.java
│       │   │   │   ├── 📄 ChatMessage.java
│       │   │   │   ├── 📄 Order.java
│       │   │   │   ├── 📄 OrderItem.java
│       │   │   │   ├── 📄 PaymentMethod.java
│       │   │   │   ├── 📄 Product.java
│       │   │   │   ├── 📄 User.java
│       │   │   │   ├── 📄 WishlistItem.java
│       │   │   │   └── 📄 WishlistItemEntity.java
│       │   │   ├── 📄 Port8080Guard.java
│       │   │   ├── 📂 repository/
│       │   │   │   ├── 📄 CartItemRepository.java
│       │   │   │   ├── 📄 ChatMessageRepository.java
│       │   │   │   ├── 📄 OrderRepository.java
│       │   │   │   ├── 📄 PaymentMethodRepository.java
│       │   │   │   ├── 📄 ProductRepository.java
│       │   │   │   ├── 📄 UserRepository.java
│       │   │   │   └── 📄 WishlistItemRepository.java
│       │   │   ├── 📂 service/
│       │   │   │   ├── 📄 AuthService.java
│       │   │   │   ├── 📄 CartService.java
│       │   │   │   ├── 📄 ChatService.java
│       │   │   │   ├── 📄 CustomUserDetailsService.java
│       │   │   │   ├── 📄 OrderService.java
│       │   │   │   ├── 📄 OrderValidationException.java
│       │   │   │   ├── 📄 PaymentMethodService.java
│       │   │   │   └── 📄 WishlistService.java
│       │   │   ├── 📂 support/
│       │   │   │   └── 📄 StorefrontSupport.java
│       │   │   └── 📄 SmartphoneShopApplication.java
│       │   └── 📂 resources/
│       │       ├── 📄 application.properties
│       │       ├── 📄 application-dev.properties
│       │       └── 📄 application-prod.properties
│       └── 📂 test/
│           ├── 📂 java/io/github/ngtrphuc/smartphone_shop/
│           │   ├── 📂 config/
│           │   │   └── 📄 PaymentMethodSchemaInitializerTest.java
│           │   ├── 📂 controller/user/
│           │   │   ├── 📄 CompareControllerTest.java
│           │   │   └── 📄 MainControllerTest.java
│           │   ├── 📂 model/
│           │   │   └── 📄 PaymentMethodTest.java
│           │   ├── 📄 Port8080GuardTest.java
│           │   ├── 📂 service/
│           │   │   ├── 📄 AuthServiceTest.java
│           │   │   ├── 📄 CartServiceTest.java
│           │   │   ├── 📄 MockitoNullSafety.java
│           │   │   ├── 📄 OrderServiceTest.java
│           │   │   ├── 📄 PaymentMethodServiceTest.java
│           │   │   └── 📄 WishlistServiceTest.java
│           │   └── 📄 SmartphoneShopApplicationTests.java
│           └── 📂 resources/
│               └── 📄 application-test.properties
├── 📂 frontend/
│   ├── 📂 static/
│   │   ├── 📂 admin/
│   │   │   └── 📂 css/
│   │   │       └── 📄 style.css
│   │   ├── 📂 customer/
│   │   │   ├── 📂 css/
│   │   │   │   └── 📄 style.css
│   │   │   └── 📂 images/
│   │   ├── 📂 js/
│   │   │   ├── 📄 admin-shell.js
│   │   │   ├── 📄 auth-password-toggle.js
│   │   │   └── 📄 order-success.js
│   │   └── 📂 svg/
│   │       └── 📂 griddy/
│   └── 📂 templates/
│       ├── 📂 admin/
│       │   ├── 📂 error/
│       │   │   └── 📄 access-denied-admin.html
│       │   ├── 📄 chat.html
│       │   ├── 📄 dashboard.html
│       │   ├── 📄 orders.html
│       │   ├── 📄 product-form.html
│       │   └── 📄 products.html
│       └── 📂 customer/
│           ├── 📂 auth/
│           │   ├── 📄 login.html
│           │   └── 📄 register.html
│           ├── 📂 fragments/
│           │   ├── 📄 chat-widget.html
│           │   ├── 📄 compare-bar.html
│           │   └── 📄 footer.html
│           ├── 📄 cart.html
│           ├── 📄 checkout.html
│           ├── 📄 compare.html
│           ├── 📄 detail.html
│           ├── 📄 index.html
│           ├── 📄 my-orders.html
│           ├── 📄 payment-select.html
│           ├── 📄 profile.html
│           ├── 📄 shipping.html
│           ├── 📄 success.html
│           └── 📄 wishlist.html
├── 📂 scripts/
│   └── 📄 remove_product_backgrounds.py
├── 📄 .editorconfig
├── 📄 .gitattributes
├── 📄 .gitignore
├── 📄 HELP.md
├── 📄 mvnw
├── 📄 mvnw.cmd
├── 📄 pom.xml
└── 📄 README.md
```

- `backend/src/main/java/.../config`: System configuration, security, Thymeleaf, web setup, and bootstrap/schema initialization
- `backend/src/main/java/.../controller`: Request handlers for user/admin
- `backend/src/main/java/.../model`: Main entities/models
- `backend/src/main/java/.../repository`: Data access layer (Spring Data JPA)
- `backend/src/main/java/.../service`: Business logic
- `backend/src/main/java/.../support`: Shared support utilities for storefront use cases
- `backend/src/main/java/.../Port8080Guard.java`: Startup guard to ensure port `8080` is available before app boot
- `frontend/static`: Static assets (CSS, JS, images, SVG)
- `frontend/templates`: Thymeleaf views for admin/customer
- `frontend/templates/customer/fragments`: Reusable customer fragments (`chat-widget`, `compare-bar`, `footer`)
- `backend/src/test`: Unit tests and application configuration tests
- `backend/src/test/java/.../config/PaymentMethodSchemaInitializerTest.java`: Schema initializer regression tests
- `backend/src/test/java/.../Port8080GuardTest.java`: Regression tests for port guard behavior
- `scripts`: Auxiliary scripts outside the core application
- `.data`: Local H2 database files for development

## Quick Start (Default Dev Profile)

By default, the app runs with profile `dev` and uses a local file-based H2 database, so MySQL setup is not required for local testing.

### 1) Run the app

```bash
./mvnw spring-boot:run
```

Windows:

```bat
mvnw.cmd spring-boot:run
```

### 2) Access URLs

- Home: `http://localhost:8080/`
- H2 Console: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

## Bootstrap Admin Account

The app bootstraps an admin account via environment variables:

- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

Default dev values in `application-dev.properties`:

- `admin@smartphone.local`
- `Admin@123456`

You can override them before running the app.

## Run in Production (MySQL/MariaDB)

Use profile `prod` and provide:

- `DATASOURCE_URL`
- `DATASOURCE_USER`
- `DATASOURCE_PASSWORD`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

Example:

```bash
SPRING_PROFILES_ACTIVE=prod \
DATASOURCE_URL=jdbc:mysql://localhost:3306/smartphone_shop \
DATASOURCE_USER=root \
DATASOURCE_PASSWORD=your_password \
ADMIN_EMAIL=admin@yourdomain.com \
ADMIN_PASSWORD=your_strong_password \
./mvnw spring-boot:run
```

## Run Tests

```bash
./mvnw test
```

Windows:

```bat
mvnw.cmd test
```

## Security Notes

- Session cookie uses `HttpOnly`, `SameSite=Lax`
- Session fixation protection and concurrent session limits
- Security headers: CSP, frame deny, referrer policy, permissions policy
- CSRF is enabled by default for form actions
