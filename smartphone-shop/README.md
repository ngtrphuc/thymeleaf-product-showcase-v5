# Smartphone Shop

A smartphone e-commerce web application built with Spring Boot + Thymeleaf, including customer and admin flows.

## Key Features

- Sign up / sign in with `ROLE_USER` and `ROLE_ADMIN`
- Product listing, filter/search, and detail pages
- Compare, wishlist, cart, checkout, profile, and order tracking (customer)
- Product/order management and chat dashboard (admin)
- Real-time customer-admin chat with SSE
- REST API under `/api/v1/**`

## Tech Stack

- Java 21
- Spring Boot 3.5.13
- Spring Security 6
- Spring Data JPA (Hibernate)
- Thymeleaf
- Maven Wrapper (`mvnw`, `mvnw.cmd`)
- H2 (dev/test), MySQL/MariaDB (prod)

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
│       │   │   └── io/
│       │   │       └── github/
│       │   │           └── ngtrphuc/
│       │   │               └── smartphone_shop/
│       │   │                   ├── api/
│       │   │                   │   ├── dto/
│       │   │                   │   │   ├── AuthMeResponse.java
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
│       │   │                   │   └── WebConfig.java
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
│       │   │                   ├── service/
│       │   │                   │   ├── AuthService.java
│       │   │                   │   ├── CartService.java
│       │   │                   │   ├── ChatService.java
│       │   │                   │   ├── CompareService.java
│       │   │                   │   ├── CustomUserDetailsService.java
│       │   │                   │   ├── OrderService.java
│       │   │                   │   ├── OrderValidationException.java
│       │   │                   │   ├── PaymentMethodService.java
│       │   │                   │   └── WishlistService.java
│       │   │                   ├── support/
│       │   │                   │   └── StorefrontSupport.java
│       │   │                   ├── Port8080Guard.java
│       │   │                   └── SmartphoneShopApplication.java
│       │   └── resources/
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
│           │                   │   └── PaymentMethodSchemaInitializerTest.java
│           │                   ├── controller/
│           │                   │   ├── api/
│           │                   │   │   └── v1/
│           │                   │   │       ├── AuthApiControllerTest.java
│           │                   │   │       └── ProductApiControllerTest.java
│           │                   │   └── user/
│           │                   │       ├── CartControllerTest.java
│           │                   │       ├── CompareControllerTest.java
│           │                   │       └── MainControllerTest.java
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
│   │   │   ├── images/
│   │   │   └── js/
│   │   │       ├── auth-password-toggle.js
│   │   │       └── order-success.js
│   │   └── svg/
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
├── .editorconfig
├── .gitattributes
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Run (Dev)

```bash
./mvnw spring-boot:run
```

Windows:

```bat
mvnw.cmd spring-boot:run
```

By default, local runs use profile `dev` (`spring.profiles.default=dev`).  
You can still force a profile explicitly with `-Dspring-boot.run.profiles=dev|prod` or `SPRING_PROFILES_ACTIVE`.

## Access URLs (Dev)

- Home: `http://localhost:8080/`
- H2 Console: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

## Production Profile

Set env vars and run with profile `prod`:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATASOURCE_URL`
- `DATASOURCE_USER`
- `DATASOURCE_PASSWORD`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

Example:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
.\mvnw.cmd spring-boot:run
```

## Run Tests

```bash
./mvnw test
```

Windows:

```bat
mvnw.cmd test
```
