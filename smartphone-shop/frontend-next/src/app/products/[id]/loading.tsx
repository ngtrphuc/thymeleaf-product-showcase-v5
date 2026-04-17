export default function ProductDetailLoading() {
  return (
    <main className="space-y-6">
      <div className="glass-panel h-6 w-44 animate-pulse rounded-md" />
      <div className="glass-panel h-[560px] animate-pulse rounded-3xl" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="glass-panel h-64 animate-pulse rounded-2xl" />
        ))}
      </div>
    </main>
  );
}
