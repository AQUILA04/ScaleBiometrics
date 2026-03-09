import { redirect } from "next/navigation";
import { auth } from "@/lib/auth";
import { AppLayout } from "@/components/layout/app-layout";

export default async function SuperAdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();

  if (!session) {
    redirect("/superadmin/login");
  }

  const isSuperAdmin = (session.user as { role?: string })?.role === "super_admin";

  if (!isSuperAdmin) {
    redirect("/dashboard");
  }

  return <AppLayout>{children}</AppLayout>;
}
