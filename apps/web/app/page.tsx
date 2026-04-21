import { redirect } from "next/navigation";

/**
 * Root page — redirects to the dashboard.
 * Authentication is enforced by the middleware.
 */
export default function RootPage() {
  redirect("/dashboard");
}
