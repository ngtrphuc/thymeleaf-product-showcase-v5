import { AddToCartButton } from '@/components/add-to-cart-button';
import { fetchHealth, fetchProducts } from '@/lib/api';

export default async function Home() {
  const [health, products] = await Promise.all([fetchHealth(), fetchProducts()]);

  return (
    <section>
      <div className="hero">
        <p className="kicker">Overnight Replatform Sprint</p>
        <h1>Storefront migrated to Next.js App Router</h1>
        <p>
          Backend API is now served by NestJS modules. Current API status: <strong>{health}</strong>
        </p>
      </div>

      <div className="product-grid">
        {products.map((product) => (
          <article className="card" key={product.id}>
            <p className="brand">{product.brand}</p>
            <h2>{product.name}</h2>
            <p className="price">{product.price.toLocaleString('ja-JP')} JPY</p>
            <p>Stock: {product.stock}</p>
            <AddToCartButton productId={product.id} />
          </article>
        ))}
      </div>
    </section>
  );
}
