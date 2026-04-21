"use client";

import { signOut } from "next-auth/react";
import { Bell, LogOut, User, Settings, ChevronDown } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { getInitials } from "@/lib/utils";
import type { AppUser } from "@/types";

interface TopBarProps {
  user?: AppUser;
  title?: string;
}

const roleLabels: Record<string, string> = {
  superadmin: "Super Admin",
  tenant_admin: "Tenant Admin",
  api_user: "API User",
};

export function TopBar({ user, title }: TopBarProps) {
  const primaryRole = user?.roles?.[0];
  const roleLabel = primaryRole ? roleLabels[primaryRole] : "Utilisateur";
  const initials = user?.name ? getInitials(user.name) : "?";

  return (
    <header className="flex h-14 items-center gap-4 border-b border-border bg-card px-6 shadow-sm">
      {/* Page title */}
      {title && (
        <h1 className="text-sm font-semibold text-foreground truncate">
          {title}
        </h1>
      )}

      {/* Spacer */}
      <div className="flex-1" />

      {/* Notifications */}
      <button
        className="relative flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-accent hover:text-foreground transition-colors cursor-pointer"
        aria-label="Notifications"
      >
        <Bell className="h-4 w-4" />
        <span className="absolute top-1 right-1 h-2 w-2 rounded-full bg-destructive" />
      </button>

      {/* User menu */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="flex items-center gap-2.5 rounded-lg px-2 py-1.5 hover:bg-accent transition-colors cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <Avatar className="h-7 w-7">
              <AvatarImage src={user?.image ?? undefined} alt={user?.name ?? "User"} />
              <AvatarFallback className="text-xs">{initials}</AvatarFallback>
            </Avatar>
            <div className="hidden sm:block text-left min-w-0">
              <p className="text-xs font-semibold text-foreground truncate max-w-[120px]">
                {user?.name ?? "Utilisateur"}
              </p>
              <p className="text-2xs text-muted-foreground truncate">{roleLabel}</p>
            </div>
            <ChevronDown className="h-3.5 w-3.5 text-muted-foreground hidden sm:block" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuLabel>
            <div className="flex flex-col gap-0.5">
              <span className="font-semibold text-sm">{user?.name ?? "Utilisateur"}</span>
              <span className="text-xs text-muted-foreground font-normal truncate">
                {user?.email}
              </span>
            </div>
          </DropdownMenuLabel>
          {primaryRole && (
            <div className="px-2 pb-2">
              <Badge variant="info" className="text-2xs">
                {roleLabel}
              </Badge>
            </div>
          )}
          <DropdownMenuSeparator />
          <DropdownMenuItem className="gap-2">
            <User className="h-4 w-4" />
            <span>Mon profil</span>
          </DropdownMenuItem>
          <DropdownMenuItem className="gap-2">
            <Settings className="h-4 w-4" />
            <span>Paramètres</span>
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            className="gap-2 text-destructive focus:text-destructive focus:bg-destructive/10"
            onClick={() => signOut({ callbackUrl: "/login" })}
          >
            <LogOut className="h-4 w-4" />
            <span>Se déconnecter</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  );
}
