type SkeletonProps = {
  className?: string;
};

export function Skeleton({ className = "" }: SkeletonProps) {
  return <div className={`ui-skeleton rounded-xl bg-slate-200/70 ${className}`} />;
}
