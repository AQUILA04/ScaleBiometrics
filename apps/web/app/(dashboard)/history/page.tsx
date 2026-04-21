import type { Metadata } from "next";
import { History, Search, Download } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export const metadata: Metadata = {
  title: "Historique",
};

export default function HistoryPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Historique</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Journal d&apos;audit et traçabilité des opérations
          </p>
        </div>
        <Button variant="outline" size="sm" className="gap-2">
          <Download className="h-4 w-4" />
          Exporter
        </Button>
      </div>

      {/* Search bar */}
      <div className="flex gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Rechercher par RID, Trace ID..."
            className="pl-9"
          />
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Journal d&apos;audit</CardTitle>
          <CardDescription>
            Toutes les opérations avec trace ID et résultat
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex h-64 items-center justify-center rounded-lg border-2 border-dashed border-border">
            <div className="text-center">
              <History className="mx-auto h-10 w-10 text-muted-foreground/40" />
              <p className="mt-3 text-sm font-medium text-muted-foreground">
                Journal d&apos;audit immutable
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                Implémentation prévue — Epic 5.4
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
