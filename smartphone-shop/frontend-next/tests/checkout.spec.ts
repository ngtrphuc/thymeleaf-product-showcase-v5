import { expect, test, type Page } from "@playwright/test";

const corsHeaders = {
  "access-control-allow-origin": "http://localhost:3000",
  "access-control-allow-credentials": "true",
  "access-control-allow-methods": "GET,POST,PUT,DELETE,OPTIONS",
  "access-control-allow-headers": "content-type",
};

async function mockCheckoutApi(page: Page) {
  await page.route("http://localhost:8080/api/v1/**", async (route) => {
    const request = route.request();
    const method = request.method();
    const path = new URL(request.url()).pathname;

    if (method === "OPTIONS") {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    if (path === "/api/v1/cart" && method === "GET") {
      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify({
          items: [
            {
              id: 101,
              name: "Galaxy S25",
              price: 28_000_000,
              quantity: 1,
              imageUrl: null,
              availableStock: 5,
              lineTotal: 28_000_000,
              lowStock: false,
              availabilityLabel: "In stock",
            },
          ],
          totalAmount: 28_000_000,
          itemCount: 1,
          authenticated: true,
        }),
      });
      return;
    }

    if (path === "/api/v1/profile" && method === "GET") {
      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify({
          id: 1,
          email: "user@example.com",
          fullName: "Checkout User",
          phoneNumber: "0900000000",
          defaultAddress: "Tokyo, Chiyoda",
          deliveredOrderCount: 2,
          pendingOrderCount: 1,
          cartItemCount: 1,
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

    if (path === "/api/v1/orders" && method === "POST") {
      const payload = request.postDataJSON() as {
        customerName?: string;
        phoneNumber?: string;
        shippingAddress?: string;
      };
      expect(payload.customerName).toBe("Checkout User");
      expect(payload.phoneNumber).toBe("0901111111");
      expect(payload.shippingAddress).toBe("Osaka, Naniwa");

      await route.fulfill({
        status: 200,
        headers: { ...corsHeaders, "content-type": "application/json" },
        body: JSON.stringify({
          id: 9001,
          orderCode: "ORDER-9001",
          status: "PENDING",
          statusSummary: "Pending",
          customerName: payload.customerName,
          phoneNumber: payload.phoneNumber,
          shippingAddress: payload.shippingAddress,
          totalAmount: 28_000_000,
          paymentMethod: "CASH_ON_DELIVERY",
          paymentPlan: "FULL_PAYMENT",
          installmentMonths: null,
          installmentMonthlyAmount: null,
          createdAt: "2026-04-18T10:00:00",
          itemCount: 1,
          cancelable: true,
          items: [
            {
              productId: 101,
              productName: "Galaxy S25",
              price: 28_000_000,
              quantity: 1,
            },
          ],
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
}

test("checkout redirects anonymous users to login", async ({ page }) => {
  await page.goto("/checkout");
  await expect(page).toHaveURL(/\/login\?next=%2Fcheckout/);
});

test("checkout submits order successfully for authenticated user", async ({ context, page }) => {
  await context.addCookies([
    {
      name: "jwt",
      value: "header.payload.signature",
      url: "http://localhost:3000",
    },
  ]);
  await mockCheckoutApi(page);

  await page.goto("/checkout");

  await expect(page.getByRole("heading", { name: "Checkout" })).toBeVisible();

  await page.getByLabel("Phone Number").fill("0901111111");
  await page.getByLabel("Shipping Address").fill("Osaka, Naniwa");
  await page.getByRole("button", { name: "Place Order" }).click();

  await expect(page).toHaveURL(/\/checkout\/success\?code=ORDER-9001/);
  await expect(page.getByRole("heading", { name: "Order placed successfully" })).toBeVisible();
  await expect(page.getByText("ORDER-9001")).toBeVisible();
});
