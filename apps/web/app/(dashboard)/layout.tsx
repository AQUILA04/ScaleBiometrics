<<<<<<< HEAD
import { redirect } from "next/navigation";
import { auth } from "@/lib/auth";
import { AppLayout } from "@/components/layout/app-layout";

export default async function DashboardLayout({
=======
import { auth } from "@/lib/auth";
import { redirect } from "next/navigation";
import { DashboardLayout } from "@/components/layout/dashboard-layout";
import type { AppUser, UserRole } from "@/types";

export default async function ProtectedLayout({
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();

  if (!session) {
    redirect("/login");
  }

<<<<<<< HEAD
  const isSuperAdmin = (session.user as { role?: string })?.role === "super_admin";

  if (isSuperAdmin) {
    redirect("/superadmin");
  }

  return <AppLayout>{children}</AppLayout>;
=======
  const user: AppUser = {
    id: session.user?.email ?? undefined,
    name: session.user?.name ?? null,
    email: session.user?.email ?? null,
    image: session.user?.image ?? null,
    tenantId: (session.user as AppUser)?.tenantId,
    roles: ((session.user as AppUser)?.roles as UserRole[]) ?? [],
  };

  return <DashboardLayout user={user}>{children}</DashboardLayout>;
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
}
