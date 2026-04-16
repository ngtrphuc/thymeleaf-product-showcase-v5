"use client";

import { useState } from 'react';

export function AddToCartButton({ productId }: { productId: number }) {
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<string>('');

  async function onAdd() {
    setLoading(true);
    setStatus('');
    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:4000/api/v1'}/cart/items`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-user-id': 'demo-user',
          },
          body: JSON.stringify({ productId, quantity: 1 }),
        },
      );
      if (!response.ok) {
        setStatus('Failed');
      } else {
        setStatus('Added');
      }
    } catch {
      setStatus('Offline');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="cart-action">
      <button onClick={onAdd} disabled={loading}>
        {loading ? 'Adding...' : 'Add to cart'}
      </button>
      {status ? <small>{status}</small> : null}
    </div>
  );
}
