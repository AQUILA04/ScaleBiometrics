"use client";

import { useSession } from "next-auth/react";
import { Header } from "./header";
import { Sidebar } from "./sidebar";

interface AppLayoutProps {
  children: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const { data: session } = useSession();
  const isSuperAdmin = (session?.user as { role?: string })?.role === "super_admin";

  return (
    <div className="flex min-h-screen flex-col">
      <Header user={session?.user as { name?: string | null; email?: string | null; image?: string | null } | undefined} />
      <div className="flex flex-1">
        <Sidebar type={isSuperAdmin ? "superadmin" : "tenant"} />
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
