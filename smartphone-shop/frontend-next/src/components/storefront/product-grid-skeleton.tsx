import { Skeleton } from "@/components/ui/skeleton";

export function ProductGridSkeleton() {
  return (
    <div className="space-y-6">
      <section className="glass-panel rounded-3xl p-6 sm:p-8">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="mt-3 h-10 w-72 max-w-full" />
        <Skeleton className="mt-3 h-4 w-full max-w-2xl" />
      </section>

      <section className="glass-panel rounded-3xl p-5">
        <div className="grid gap-4 md:grid-cols-3">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-36 md:col-span-3" />
        </div>
      </section>

      <section className="grid grid-cols-2 gap-5 lg:grid-cols-3">
        {Array.from({ length: 8 }).map((_, index) => (
          <div key={index} className="glass-panel rounded-2xl p-4">
            <Skeleton className="h-52 w-full rounded-2xl" />
            <Skeleton className="mt-3 h-5 w-3/4" />
            <Skeleton className="mt-2 h-4 w-1/2" />
            <Skeleton className="mt-4 h-10 w-full" />
          </div>
        ))}
      </section>
    </div>
  );
}
