/**
 * Auth route group layout — no sidebar, no topbar.
 * Used for login and error pages.
 */
export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
