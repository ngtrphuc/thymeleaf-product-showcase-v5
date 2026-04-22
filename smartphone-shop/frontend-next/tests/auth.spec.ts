import { expect, test, type BrowserContext, type Route } from "@playwright/test";

const corsHeaders = {
  "access-control-allow-origin": "http://localhost:3000",
  "access-control-allow-credentials": "true",
  "access-control-allow-methods": "GET,POST,PUT,DELETE,OPTIONS",
  "access-control-allow-headers": "content-type",
};

function createFakeJwt(role: string): string {
  const header = Buffer.from(JSON.stringify({ alg: "HS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(JSON.stringify({ role, exp: 4102444800 })).toString("base64url");
  return `${header}.${payload}.signature`;
}

async function setJwtCookie(context: BrowserContext, role: string) {
  await context.addCookies([
    {
      name: "jwt",
      value: createFakeJwt(role),
      url: "http://localhost:3000",
      httpOnly: true,
      sameSite: "Lax",
    },
  ]);
}

async function fulfillOptions(route: Route) {
  await route.fulfill({ status: 204, headers: corsHeaders });
}

function createCheckoutCartResponse() {
  return {
    items: [
      {
        id: 301,
        name: "Galaxy S25 Ultra",
        price: 31_000_000,
        quantity: 1,
        imageUrl: "/images/galaxy-s25-ultra.png",
        availableStock: 8,
        lineTotal: 31_000_000,
        lowStock: false,
        availabilityLabel: "In stock",
      },
    ],
    totalAmount: 31_000_000,
    itemCount: 1,
    authenticated: true,
  };
}

test("customer login preserves next path for protected checkout route", async ({ context, page }) => {
  await page.route("http://localhost:8080/api/v1/**", async (route) => {
    const request = route.request();
    const method = request.method();
    const path = new URL(request.url()).pathname;

    if (method === "OPTIONS") {
      await fulfillOptions(route);
      return;
    }

    if (path === "/api/v1/auth/login" && method === "POST") {
      await setJwtCookie(context, "ROLE_USER");
      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify({
          accessToken: "mock-token",
          tokenType: "Bearer",
          expiresInSeconds: 7200,
          email: "user@example.com",
          role: "ROLE_USER",
          fullName: "Customer User",
        }),
      });
      return;
    }

    if (path === "/api/v1/cart" && method === "GET") {
      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify(createCheckoutCartResponse()),
      });
      return;
    }

    if (path === "/api/v1/profile" && method === "GET") {
      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify({
          id: 11,
          email: "user@example.com",
          fullName: "Customer User",
          phoneNumber: null,
          defaultAddress: null,
          deliveredOrderCount: 0,
          pendingOrderCount: 0,
          cartItemCount: 0,
          paymentMethods: [],
        }),
      });
      return;
    }

    if (path === "/api/v1/payment-methods" && method === "GET") {
      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify([]),
      });
      return;
    }

    await route.fulfill({
      status: 404,
      headers: { ...corsHeaders, "content-type": "application/json" },
      body: JSON.stringify({ message: `Unmocked endpoint: ${method} ${path}` }),
    });
  });

  await page.goto("/login?next=%2Fcheckout");
  await page.getByLabel("Email").fill("user@example.com");
  await page.locator('input[name="password"]').fill("UserPass123!");
  await page.getByRole("button", { name: "Sign In" }).click();

  await expect(page).toHaveURL(/\/checkout/);
  await expect(page.getByRole("heading", { name: "Checkout" })).toBeVisible();
});

test("admin login redirects to admin dashboard", async ({ context, page }) => {
  await page.route("http://localhost:8080/api/v1/**", async (route) => {
    const request = route.request();
    const method = request.method();
    const path = new URL(request.url()).pathname;

    if (method === "OPTIONS") {
      await fulfillOptions(route);
      return;
    }

    if (path === "/api/v1/auth/login" && method === "POST") {
      await setJwtCookie(context, "ROLE_ADMIN");
      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify({
          accessToken: "mock-token",
          tokenType: "Bearer",
          expiresInSeconds: 7200,
          email: "admin@example.com",
          role: "ROLE_ADMIN",
          fullName: "Administrator",
        }),
      });
      return;
    }

    if (path === "/api/v1/admin/dashboard" && method === "GET") {
      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify({
          totalProducts: 10,
          totalItemsSold: 20,
          totalOrders: 5,
          totalRevenue: 123456,
          currentPage: 0,
          totalPages: 1,
          recentOrders: [],
        }),
      });
      return;
    }

    await route.fulfill({
      status: 404,
      headers: { ...corsHeaders, "content-type": "application/json" },
      body: JSON.stringify({ message: `Unmocked endpoint: ${method} ${path}` }),
    });
  });

  await page.goto("/login");
  await page.getByLabel("Email").fill("admin@example.com");
  await page.locator('input[name="password"]').fill("AdminPass123!");
  await page.getByRole("button", { name: "Sign In" }).click();

  await expect(page).toHaveURL(/\/admin/);
  await expect(page.getByRole("heading", { name: "Admin Dashboard" })).toBeVisible();
});
