"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
<<<<<<< HEAD
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
=======
import {
  LayoutDashboard,
  ListOrdered,
  History,
  Settings,
  Users,
  Activity,
  Fingerprint,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useState } from "react";

interface NavItem {
  href: string;
  label: string;
  icon: React.ElementType;
  badge?: string;
  roles?: string[];
}

const navItems: NavItem[] = [
  {
    href: "/dashboard",
    label: "Dashboard",
    icon: LayoutDashboard,
  },
  {
    href: "/queue",
    label: "File d'attente",
    icon: ListOrdered,
  },
  {
    href: "/history",
    label: "Historique",
    icon: History,
  },
  {
    href: "/configuration",
    label: "Configuration",
    icon: Settings,
  },
  {
    href: "/tenants",
    label: "Tenants",
    icon: Users,
    roles: ["superadmin"],
  },
];

const bottomItems: NavItem[] = [
  {
    href: "/health",
    label: "Santé système",
    icon: Activity,
  },
];

interface SidebarProps {
  userRoles?: string[];
}

export function Sidebar({ userRoles = [] }: SidebarProps) {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  const isActive = (href: string) => {
    if (href === "/dashboard") return pathname === "/dashboard";
    return pathname.startsWith(href);
  };

  const visibleItems = navItems.filter(
    (item) => !item.roles || item.roles.some((r) => userRoles.includes(r))
  );

  return (
    <aside
      className={cn(
        "relative flex flex-col h-full bg-sidebar-bg text-sidebar-fg transition-all duration-300 ease-in-out shadow-sidebar",
        collapsed ? "w-16" : "w-[260px]"
      )}
    >
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-white/10">
        <div className="flex-shrink-0 w-9 h-9 rounded-xl bg-primary flex items-center justify-center shadow-lg">
          <Fingerprint className="w-5 h-5 text-white" />
        </div>
        {!collapsed && (
          <div className="min-w-0">
            <p className="text-sm font-bold text-white truncate">ScaleBiometrics</p>
            <p className="text-xs text-sidebar-muted truncate">Admin Console</p>
          </div>
        )}
      </div>

      {/* Collapse toggle */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3 top-[4.5rem] z-10 flex h-6 w-6 items-center justify-center rounded-full border border-border bg-card text-muted-foreground shadow-sm hover:bg-accent hover:text-accent-foreground transition-colors cursor-pointer"
        aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
      >
        {collapsed ? (
          <ChevronRight className="h-3 w-3" />
        ) : (
          <ChevronLeft className="h-3 w-3" />
        )}
      </button>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-4 px-2 scrollbar-thin">
        {!collapsed && (
          <p className="px-3 mb-2 text-2xs font-semibold uppercase tracking-widest text-sidebar-muted">
            Navigation
          </p>
        )}
        <ul className="space-y-0.5">
          {visibleItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.href);
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150 cursor-pointer",
                    active
                      ? "bg-sidebar-active-bg text-sidebar-active-fg shadow-sm"
                      : "text-sidebar-fg hover:bg-sidebar-hover-bg hover:text-white"
                  )}
                  title={collapsed ? item.label : undefined}
                >
                  <Icon
                    className={cn(
                      "flex-shrink-0 h-4.5 w-4.5",
                      active ? "text-white" : "text-sidebar-muted"
                    )}
                    aria-hidden="true"
                  />
                  {!collapsed && (
                    <span className="truncate">{item.label}</span>
                  )}
                  {!collapsed && item.badge && (
                    <span className="ml-auto flex-shrink-0 rounded-full bg-primary/20 px-2 py-0.5 text-2xs font-semibold text-primary-light">
                      {item.badge}
                    </span>
                  )}
                </Link>
              </li>
            );
          })}
        </ul>

        {/* Bottom section */}
        <div className="mt-4 pt-4 border-t border-white/10">
          {!collapsed && (
            <p className="px-3 mb-2 text-2xs font-semibold uppercase tracking-widest text-sidebar-muted">
              Système
            </p>
          )}
          <ul className="space-y-0.5">
            {bottomItems.map((item) => {
              const Icon = item.icon;
              const active = isActive(item.href);
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className={cn(
                      "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150 cursor-pointer",
                      active
                        ? "bg-sidebar-active-bg text-sidebar-active-fg"
                        : "text-sidebar-fg hover:bg-sidebar-hover-bg hover:text-white"
                    )}
                    title={collapsed ? item.label : undefined}
                  >
                    <Icon
                      className={cn(
                        "flex-shrink-0 h-4 w-4",
                        active ? "text-white" : "text-sidebar-muted"
                      )}
                      aria-hidden="true"
                    />
                    {!collapsed && (
                      <span className="truncate">{item.label}</span>
                    )}
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      </nav>

      {/* Version */}
      {!collapsed && (
        <div className="px-4 py-3 border-t border-white/10">
          <p className="text-2xs text-sidebar-muted">v0.1.0 — Epic 5</p>
        </div>
      )}
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
    </aside>
  );
}
