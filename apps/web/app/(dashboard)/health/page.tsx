import type { Metadata } from "next";
import { Activity, Server, Database, Cpu, HardDrive } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export const metadata: Metadata = {
  title: "Santé système",
};

interface HealthComponentProps {
  name: string;
  icon: React.ElementType;
  status: "UP" | "DOWN" | "UNKNOWN";
  description: string;
}

function HealthComponent({ name, icon: Icon, status, description }: HealthComponentProps) {
  return (
    <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4">
      <div
        className={`rounded-xl p-2.5 ${
          status === "UP"
            ? "bg-emerald-100 dark:bg-emerald-900/30"
            : status === "DOWN"
            ? "bg-red-100 dark:bg-red-900/30"
            : "bg-slate-100 dark:bg-slate-800"
        }`}
      >
        <Icon
          className={`h-5 w-5 ${
            status === "UP"
              ? "text-emerald-600 dark:text-emerald-400"
              : status === "DOWN"
              ? "text-red-600 dark:text-red-400"
              : "text-slate-500"
          }`}
        />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-foreground">{name}</p>
        <p className="text-xs text-muted-foreground truncate">{description}</p>
      </div>
      <Badge
        variant={
          status === "UP"
            ? "success"
            : status === "DOWN"
            ? "destructive"
            : "neutral"
        }
      >
        {status === "UP" ? "Opérationnel" : status === "DOWN" ? "Hors ligne" : "Inconnu"}
      </Badge>
    </div>
  );
}

export default function HealthPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Santé système</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            État des composants de la plateforme
          </p>
        </div>
        <Badge variant="neutral" className="gap-1.5">
          <Activity className="h-3 w-3" />
          Données en temps réel — Epic 5.7
        </Badge>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Composants système</CardTitle>
          <CardDescription>
            Vérification approfondie de tous les services
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <HealthComponent
            name="API Gateway"
            icon={Server}
            status="UNKNOWN"
            description="Point d'entrée REST/gRPC/Kafka"
          />
          <HealthComponent
            name="PostgreSQL"
            icon={Database}
            status="UNKNOWN"
            description="Base de données multi-tenant"
          />
          <HealthComponent
            name="Apache Kafka"
            icon={Activity}
            status="UNKNOWN"
            description="Bus d'événements distribué"
          />
          <HealthComponent
            name="Workers"
            icon={Cpu}
            status="UNKNOWN"
            description="Nœuds de matching biométrique"
          />
          <HealthComponent
            name="MinIO"
            icon={HardDrive}
            status="UNKNOWN"
            description="Stockage d'images biométriques"
          />
        </CardContent>
      </Card>
    </div>
  );
}
