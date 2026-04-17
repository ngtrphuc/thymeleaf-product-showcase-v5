export default function ProductsLoading() {
  return (
    <main className="space-y-5">
      <div className="glass-panel h-36 animate-pulse rounded-3xl" />
      <div className="glass-panel h-28 animate-pulse rounded-3xl" />
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, index) => (
          <div key={index} className="glass-panel h-[370px] animate-pulse rounded-3xl" />
        ))}
      </div>
    </main>
  );
}
