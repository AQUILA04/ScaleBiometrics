import { auth } from "@/lib/auth";
import { redirect } from "next/navigation";
import { DashboardLayout } from "@/components/layout/dashboard-layout";
import type { AppUser, UserRole } from "@/types";

export default async function ProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();

  if (!session) {
    redirect("/login");
  }

  const user: AppUser = {
    id: session.user?.email ?? undefined,
    name: session.user?.name ?? null,
    email: session.user?.email ?? null,
    image: session.user?.image ?? null,
    tenantId: (session.user as AppUser)?.tenantId,
    roles: ((session.user as AppUser)?.roles as UserRole[]) ?? [],
  };

  return <DashboardLayout user={user}>{children}</DashboardLayout>;
}
