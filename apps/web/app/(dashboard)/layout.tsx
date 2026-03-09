import { redirect } from "next/navigation";
import { auth } from "@/lib/auth";
import { AppLayout } from "@/components/layout/app-layout";

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();

  if (!session) {
    redirect("/login");
  }

  const isSuperAdmin = (session.user as { role?: string })?.role === "super_admin";

  if (isSuperAdmin) {
    redirect("/superadmin");
  }

  return <AppLayout>{children}</AppLayout>;
}
