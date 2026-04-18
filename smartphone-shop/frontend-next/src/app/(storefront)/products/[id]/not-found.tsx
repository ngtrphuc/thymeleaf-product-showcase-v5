export default function ProductNotFound() {
  return (
    <div className="glass-panel rounded-3xl p-8 text-center">
      <h1 className="text-2xl font-bold text-slate-900">Product not found</h1>
      <p className="mt-2 text-sm text-slate-600">The requested product does not exist or was removed.</p>
    </div>
  );
}
