"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  ListOrdered,
  FileText,
  Settings,
  Key,
  Webhook,
  Users,
  Activity,
  Server,
  Shield,
} from "lucide-react";

const tenantNavItems = [
  {
    title: "Dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    title: "Queue",
    href: "/queue",
    icon: ListOrdered,
  },
  {
    title: "Records",
    href: "/records",
    icon: FileText,
  },
  {
    title: "Settings",
    href: "/settings",
    icon: Settings,
  },
];

const tenantSettingsNav = [
  {
    title: "API Keys",
    href: "/settings/api-keys",
    icon: Key,
  },
  {
    title: "Webhooks",
    href: "/settings/webhooks",
    icon: Webhook,
  },
];

const superAdminNavItems = [
  {
    title: "Dashboard",
    href: "/superadmin",
    icon: LayoutDashboard,
  },
  {
    title: "Tenants",
    href: "/superadmin/tenants",
    icon: Users,
  },
  {
    title: "Infrastructure",
    href: "/superadmin/infrastructure",
    icon: Server,
  },
  {
    title: "Workers",
    href: "/superadmin/workers",
    icon: Activity,
  },
  {
    title: "Audit Logs",
    href: "/superadmin/audit-logs",
    icon: Shield,
  },
];

interface SidebarProps {
  type?: "tenant" | "superadmin";
}

export function Sidebar({ type = "tenant" }: SidebarProps) {
  const pathname = usePathname();
  const navItems = type === "superadmin" ? superAdminNavItems : tenantNavItems;
  const settingsNav = type === "tenant" ? tenantSettingsNav : [];

  return (
    <aside className="hidden w-64 flex-col border-r bg-card md:flex">
      <nav className="flex-1 space-y-1 p-4">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
              pathname === item.href
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
            )}
          >
            <item.icon className="h-5 w-5" />
            {item.title}
          </Link>
        ))}

        {settingsNav.length > 0 && (
          <>
            <div className="my-4 border-t" />
            <p className="mb-2 px-3 text-xs font-semibold text-muted-foreground">
              Settings
            </p>
            {settingsNav.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  pathname === item.href
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                )}
              >
                <item.icon className="h-5 w-5" />
                {item.title}
              </Link>
            ))}
          </>
        )}
      </nav>
    </aside>
  );
}
