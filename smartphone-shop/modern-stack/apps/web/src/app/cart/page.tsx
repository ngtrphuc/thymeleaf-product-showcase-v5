"use client";

import { useEffect, useState } from 'react';

type CartLine = { productId: number; quantity: number };

export default function CartPage() {
  const [items, setItems] = useState<CartLine[]>([]);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:4000/api/v1'}/cart`, {
      headers: { 'x-user-id': 'demo-user' },
      cache: 'no-store',
    })
      .then((res) => res.json())
      .then((body) => setItems(body?.data?.items ?? []))
      .catch(() => setItems([]));
  }, []);

  return (
    <section>
      <h1>Cart</h1>
      {items.length === 0 ? <p>Cart is empty.</p> : null}
      {items.map((line) => (
        <div key={line.productId} className="card">
          Product #{line.productId} - Qty: {line.quantity}
        </div>
      ))}
    </section>
  );
}
