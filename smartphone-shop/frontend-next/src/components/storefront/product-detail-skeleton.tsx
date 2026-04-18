import { Skeleton } from "@/components/ui/skeleton";

export function ProductDetailSkeleton() {
  return (
    <div className="grid gap-6 lg:grid-cols-[1.1fr_1fr]">
      <section className="glass-panel rounded-3xl p-6">
        <Skeleton className="h-80 w-full rounded-2xl" />
      </section>
      <section className="glass-panel rounded-3xl p-6">
        <Skeleton className="h-8 w-3/4" />
        <Skeleton className="mt-3 h-6 w-1/3" />
        <Skeleton className="mt-4 h-4 w-full" />
        <Skeleton className="mt-2 h-4 w-11/12" />
        <Skeleton className="mt-6 h-11 w-48" />
      </section>
    </div>
  );
}
