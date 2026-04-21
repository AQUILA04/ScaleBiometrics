import type { Metadata } from "next";
import {
  Activity,
  Clock,
  CheckCircle2,
  AlertTriangle,
  TrendingUp,
  Users,
  Fingerprint,
  Zap,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export const metadata: Metadata = {
  title: "Dashboard",
};

interface MetricCardProps {
  title: string;
  value: string;
  change?: string;
  changeType?: "positive" | "negative" | "neutral";
  icon: React.ElementType;
  iconColor: string;
  description?: string;
}

function MetricCard({
  title,
  value,
  change,
  changeType = "neutral",
  icon: Icon,
  iconColor,
  description,
}: MetricCardProps) {
  return (
    <Card className="metric-card">
      <CardContent className="p-6">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <p className="text-sm font-medium text-muted-foreground">{title}</p>
            <p className="text-2xl font-bold text-foreground">{value}</p>
            {description && (
              <p className="text-xs text-muted-foreground">{description}</p>
            )}
          </div>
          <div className={`rounded-xl p-2.5 ${iconColor}`}>
            <Icon className="h-5 w-5 text-white" aria-hidden="true" />
          </div>
        </div>
        {change && (
          <div className="mt-3 flex items-center gap-1.5">
            <TrendingUp
              className={`h-3.5 w-3.5 ${
                changeType === "positive"
                  ? "text-emerald-500"
                  : changeType === "negative"
                  ? "text-red-500"
                  : "text-muted-foreground"
              }`}
            />
            <span
              className={`text-xs font-medium ${
                changeType === "positive"
                  ? "text-emerald-600 dark:text-emerald-400"
                  : changeType === "negative"
                  ? "text-red-600 dark:text-red-400"
                  : "text-muted-foreground"
              }`}
            >
              {change}
            </span>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Vue d&apos;ensemble de la plateforme biométrique
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="success" className="gap-1.5">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Système opérationnel
          </Badge>
        </div>
      </div>

      {/* Metric cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title="Requêtes en attente"
          value="—"
          icon={Clock}
          iconColor="bg-amber-500"
          description="File d'attente globale"
          change="Données en temps réel"
          changeType="neutral"
        />
        <MetricCard
          title="Latence P95"
          value="—"
          icon={Zap}
          iconColor="bg-blue-500"
          description="Objectif : < 2 000ms"
          change="SLO cible"
          changeType="neutral"
        />
        <MetricCard
          title="Taux de succès"
          value="—"
          icon={CheckCircle2}
          iconColor="bg-emerald-500"
          description="Dernières 24h"
          change="Disponibilité"
          changeType="neutral"
        />
        <MetricCard
          title="Workers actifs"
          value="—"
          icon={Activity}
          iconColor="bg-purple-500"
          description="Nœuds de traitement"
          change="Capacité cluster"
          changeType="neutral"
        />
      </div>

      {/* Secondary metrics */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <MetricCard
          title="Identités enregistrées"
          value="—"
          icon={Fingerprint}
          iconColor="bg-indigo-500"
          description="Total toutes tenants"
        />
        <MetricCard
          title="Tenants actifs"
          value="—"
          icon={Users}
          iconColor="bg-teal-500"
          description="Organisations connectées"
        />
        <MetricCard
          title="Alertes actives"
          value="—"
          icon={AlertTriangle}
          iconColor="bg-red-500"
          description="Incidents en cours"
        />
      </div>

      {/* Placeholder charts section */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Débit (req/s)</CardTitle>
            <CardDescription>Évolution sur les dernières 24h</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex h-48 items-center justify-center rounded-lg border-2 border-dashed border-border">
              <div className="text-center">
                <TrendingUp className="mx-auto h-8 w-8 text-muted-foreground/40" />
                <p className="mt-2 text-sm text-muted-foreground">
                  Graphique de débit — Epic 5.2
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Distribution de latence</CardTitle>
            <CardDescription>P50 / P95 / P99 en millisecondes</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex h-48 items-center justify-center rounded-lg border-2 border-dashed border-border">
              <div className="text-center">
                <Activity className="mx-auto h-8 w-8 text-muted-foreground/40" />
                <p className="mt-2 text-sm text-muted-foreground">
                  Graphique de latence — Epic 5.2
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
