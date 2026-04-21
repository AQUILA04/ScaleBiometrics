import type { Metadata } from "next";
import { ListOrdered, Filter, RefreshCw } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export const metadata: Metadata = {
  title: "File d'attente",
};

export default function QueuePage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">File d&apos;attente</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Gestion des jobs de déduplication et d&apos;identification
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" className="gap-2">
            <Filter className="h-4 w-4" />
            Filtrer
          </Button>
          <Button variant="outline" size="sm" className="gap-2">
            <RefreshCw className="h-4 w-4" />
            Actualiser
          </Button>
        </div>
      </div>

      {/* Status summary */}
      <div className="flex flex-wrap gap-3">
        <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2.5">
          <span className="h-2 w-2 rounded-full bg-amber-400" />
          <span className="text-sm font-medium">En attente</span>
          <Badge variant="warning">—</Badge>
        </div>
        <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2.5">
          <span className="h-2 w-2 rounded-full bg-blue-400 animate-pulse" />
          <span className="text-sm font-medium">En traitement</span>
          <Badge variant="info">—</Badge>
        </div>
        <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2.5">
          <span className="h-2 w-2 rounded-full bg-red-400" />
          <span className="text-sm font-medium">Échecs</span>
          <Badge variant="destructive">—</Badge>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Jobs actifs</CardTitle>
          <CardDescription>
            Tableau de bord en temps réel des jobs de biométrie
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex h-64 items-center justify-center rounded-lg border-2 border-dashed border-border">
            <div className="text-center">
              <ListOrdered className="mx-auto h-10 w-10 text-muted-foreground/40" />
              <p className="mt-3 text-sm font-medium text-muted-foreground">
                Grille de données en temps réel
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                Implémentation prévue — Epic 5.3
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
