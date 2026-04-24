export default function StorefrontTemplate({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return <div className="page-enter">{children}</div>;
}
