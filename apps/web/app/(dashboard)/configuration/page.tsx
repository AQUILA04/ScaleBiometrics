import type { Metadata } from "next";
import { Settings, Key, Webhook, Shield } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";

export const metadata: Metadata = {
  title: "Configuration",
};

interface ConfigSectionProps {
  icon: React.ElementType;
  iconColor: string;
  title: string;
  description: string;
  comingSoon?: string;
}

function ConfigSection({
  icon: Icon,
  iconColor,
  title,
  description,
  comingSoon,
}: ConfigSectionProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className={`rounded-xl p-2.5 ${iconColor}`}>
            <Icon className="h-5 w-5 text-white" />
          </div>
          <div>
            <CardTitle className="text-base">{title}</CardTitle>
            <CardDescription className="text-xs mt-0.5">{description}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="flex h-32 items-center justify-center rounded-lg border-2 border-dashed border-border">
          <p className="text-xs text-muted-foreground">
            {comingSoon ?? "Implémentation prévue — Epic 5.5"}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}

export default function ConfigurationPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Configuration</h1>
        <p className="text-sm text-muted-foreground mt-0.5">
          Clés API, webhooks et paramètres de sécurité
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ConfigSection
          icon={Key}
          iconColor="bg-blue-500"
          title="Clés API"
          description="Gérer les clés d'accès programmatique"
        />
        <ConfigSection
          icon={Webhook}
          iconColor="bg-purple-500"
          title="Webhooks"
          description="Configurer les notifications d'événements"
        />
        <ConfigSection
          icon={Shield}
          iconColor="bg-emerald-500"
          title="Sécurité"
          description="Politiques d'accès et restrictions IP"
        />
        <ConfigSection
          icon={Settings}
          iconColor="bg-slate-500"
          title="Paramètres avancés"
          description="Seuils de correspondance et limites de débit"
        />
      </div>
    </div>
  );
}
