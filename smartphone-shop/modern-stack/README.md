# Smartphone Shop Modern (Overnight Track)

This workspace is the accelerated migration target from the legacy Spring Boot + Thymeleaf app.

## Stack
- Frontend: Next.js App Router + TypeScript + Tailwind
- Backend: NestJS + Fastify + class-validator
- Shared types: `@smartphone-shop/shared`
- Infra: Docker Compose (PostgreSQL + Redis + API + Web)

## Workspace layout
- `apps/web`: storefront/admin frontend shell
- `apps/api`: backend API modules (`health`, `auth`, `products`, `cart`, `orders`)
- `packages/shared`: shared contracts/types
- `infra/docker-compose.yml`: local full-stack runtime

## Quick start (local without Docker)
```powershell
cd modern-stack
npm install
npm run dev:api
npm run dev:web
```

- API: `http://localhost:4000/api/v1/health`
- Web: `http://localhost:3000`

## Quick start (Docker)
```powershell
cd modern-stack
npm run docker:up
```

## Migration status (overnight)
- [x] New mono-workspace scaffolded
- [x] NestJS Fastify foundation with domain modules
- [x] Next.js storefront/admin shell
- [x] Shared package for contracts
- [x] Docker infra with Postgres + Redis
- [ ] Real auth (JWT + refresh + RBAC persistence)
- [ ] DB layer (Prisma migrations + repositories)
- [ ] Business parity with legacy flows

## API endpoints shipped in this sprint
- `GET /api/v1/health`
- `POST /api/v1/auth/login`
- `GET /api/v1/products?keyword=`
- `GET /api/v1/cart` (header `x-user-id`)
- `POST /api/v1/cart/items` (header `x-user-id`)
- `DELETE /api/v1/cart` (header `x-user-id`)
- `GET /api/v1/orders` (header `x-user-id`)
- `POST /api/v1/orders` (header `x-user-id`)

## Legacy migration mapping
- Legacy `controller/api/v1/*` -> `apps/api/src/modules/*/*controller.ts`
- Legacy `service/*` -> `apps/api/src/modules/*/*service.ts`
- Legacy `frontend/templates/customer/*` -> `apps/web/src/app/*`
- Legacy `frontend/templates/admin/*` -> `apps/web/src/app/admin/*`

## Next wave after overnight
1. Replace in-memory cart/order with PostgreSQL + Redis.
2. Port payment/wishlist/compare/chat modules one-by-one.
3. Add integration tests and contract tests.
4. Cut over routes from legacy app to Next/Nest gateway.
