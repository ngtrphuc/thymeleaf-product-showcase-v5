type GriddyIconProps = {
  name: string;
  className?: string;
};

export function GriddyIcon({ name, className }: GriddyIconProps) {
  return <span aria-hidden className={`ui-icon-mask ui-icon-${name} ${className ?? ""}`.trim()} />;
}
