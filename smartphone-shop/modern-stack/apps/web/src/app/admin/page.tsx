import { fetchHealth } from '@/lib/api';

export default async function AdminPage() {
  const health = await fetchHealth();

  return (
    <section>
      <h1>Admin Control Room</h1>
      <p>API health: {health}</p>
      <ul>
        <li>Orders moderation: TODO</li>
        <li>Catalog management: TODO</li>
        <li>Live chat monitor: TODO</li>
      </ul>
    </section>
  );
}
