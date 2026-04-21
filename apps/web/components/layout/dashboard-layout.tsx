"use client";

import { Sidebar } from "./sidebar";
import { TopBar } from "./topbar";
import type { AppUser } from "@/types";

interface DashboardLayoutProps {
  children: React.ReactNode;
  user?: AppUser;
  pageTitle?: string;
}

export function DashboardLayout({ children, user, pageTitle }: DashboardLayoutProps) {
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Sidebar */}
      <Sidebar userRoles={user?.roles ?? []} />

      {/* Main content area */}
      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">
        {/* Top bar */}
        <TopBar user={user} title={pageTitle} />

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-6 scrollbar-thin">
          <div className="mx-auto max-w-7xl animate-in">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
