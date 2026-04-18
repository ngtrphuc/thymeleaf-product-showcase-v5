import type { CSSProperties } from "react";

type GriddyIconProps = {
  name: string;
  className?: string;
};

export function GriddyIcon({ name, className }: GriddyIconProps) {
  const iconStyle = { "--icon-url": `url(/griddy/${name}.svg)` } as CSSProperties;
  return <span aria-hidden className={`ui-icon-mask ${className ?? ""}`.trim()} style={iconStyle} />;
}
