import { Skeleton } from "@/components/ui/skeleton";

export function CheckoutSkeleton() {
  return (
    <div className="space-y-6">
      <section className="glass-panel rounded-3xl p-6">
        <Skeleton className="h-8 w-40" />
        <Skeleton className="mt-3 h-4 w-80 max-w-full" />
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="glass-panel space-y-4 rounded-3xl p-6">
          <Skeleton className="h-6 w-44" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-6 w-28" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>

        <div className="glass-panel space-y-4 rounded-3xl p-6">
          <Skeleton className="h-6 w-36" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-5/6" />
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-11 w-full" />
        </div>
      </section>
    </div>
  );
}
