# frontend-next

Next.js App Router frontend for the Smartphone Shop project.

## Local development

```bash
npm install
npm.cmd run dev
```

Open `http://localhost:3000`.

PowerShell note:

- If `npm.ps1` is blocked by execution policy, run `npm.cmd` commands instead of `npm`.

## Scripts

- `npm.cmd run dev`: start dev server with webpack (`next dev --webpack`, default and recommended).
- `npm.cmd run dev:turbo`: start dev server with Turbopack.
- `npm.cmd run build`: production build.
- `npm.cmd run start`: start production server.
- `npm.cmd run lint`: lint frontend source.
- `npm.cmd run test:e2e`: run Playwright tests.

## Environment

Create `frontend-next/.env.local` and set:

- `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`

## Recent storefront UI updates

- Added route-level page transitions for App Router templates:
  - `src/app/(storefront)/template.tsx`
  - `src/app/admin/template.tsx`
- Refined motion and interaction behavior in `src/app/globals.css`:
  transform/opacity-first transitions for smoother rendering.
- Product catalog pagination rail (`src/components/storefront/catalog-paged-grid.tsx`):
  - desktop: vertical sticky rail (summary-like),
  - mobile/tablet: horizontal fallback,
  - up/down arrow page navigation,
  - active page button ignores hover motion.
- Storefront footer (`src/components/storefront/storefront-footer.tsx`):
  - English copy and hotline: `+81 XXXX XXXX`,
  - role-aware quick links:
    - guest: products + compare,
    - authenticated customer: adds cart + orders,
    - authenticated admin: admin panel link,
  - SNS icon buttons with external login links and tooltip labels shown on hover.

## Route protection and proxy

- This project uses Next.js 16 route proxy entry at `src/proxy.ts`.
- `proxy.ts` is expected for this version; `middleware.ts` is not required here.

## Troubleshooting

- `ERR_CONNECTION_REFUSED` on `localhost:3000`:
  - Ensure this app is running and logs `Ready`.
  - Run `npm.cmd run dev` from `frontend-next`.
- UI loads but API fails:
  - Verify backend is running at `http://localhost:8080`.
  - Re-check `NEXT_PUBLIC_API_BASE_URL` in `.env.local`.
