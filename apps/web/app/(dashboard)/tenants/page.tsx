import type { Metadata } from "next";
import { Users, Plus, Search } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";

export const metadata: Metadata = {
  title: "Tenants",
};

export default function TenantsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Tenants</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Gestion des organisations et provisionnement
          </p>
        </div>
        <Button size="sm" className="gap-2">
          <Plus className="h-4 w-4" />
          Nouveau tenant
        </Button>
      </div>

      {/* Stats */}
      <div className="flex flex-wrap gap-3">
        <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2.5">
          <span className="h-2 w-2 rounded-full bg-emerald-400" />
          <span className="text-sm font-medium">Actifs</span>
          <Badge variant="success">—</Badge>
        </div>
        <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2.5">
          <span className="h-2 w-2 rounded-full bg-amber-400" />
          <span className="text-sm font-medium">En attente</span>
          <Badge variant="warning">—</Badge>
        </div>
        <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2.5">
          <span className="h-2 w-2 rounded-full bg-slate-400" />
          <span className="text-sm font-medium">Suspendus</span>
          <Badge variant="neutral">—</Badge>
        </div>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input placeholder="Rechercher un tenant..." className="pl-9" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Liste des tenants</CardTitle>
          <CardDescription>
            Toutes les organisations provisionnées sur la plateforme
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex h-64 items-center justify-center rounded-lg border-2 border-dashed border-border">
            <div className="text-center">
              <Users className="mx-auto h-10 w-10 text-muted-foreground/40" />
              <p className="mt-3 text-sm font-medium text-muted-foreground">
                Tableau de gestion des tenants
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                Implémentation prévue — Epic 5.6
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
