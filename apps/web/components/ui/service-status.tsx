import { cn } from "@/lib/utils";
import { StatusBadge } from "./status-badge";
import { CheckCircle, XCircle, AlertCircle, Clock } from "lucide-react";
import type { ServiceHealth } from "@/types/dashboard";

interface ServiceStatusProps {
  services: ServiceHealth[];
  className?: string;
}

export function ServiceStatus({ services, className }: ServiceStatusProps) {
  const getStatusIcon = (status: ServiceHealth["status"]) => {
    switch (status) {
      case "healthy":
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case "degraded":
        return <AlertCircle className="h-4 w-4 text-yellow-500" />;
      case "down":
        return <XCircle className="h-4 w-4 text-red-500" />;
    }
  };

  return (
    <div className={cn("space-y-3", className)}>
      {services.map((service) => (
        <div
          key={service.name}
          className="flex items-center justify-between rounded-lg border p-3"
        >
          <div className="flex items-center gap-3">
            {getStatusIcon(service.status)}
            <div>
              <p className="font-medium">{service.name}</p>
              <p className="text-xs text-muted-foreground">
                Last check: {new Date(service.lastCheck).toLocaleTimeString()}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            {service.latency !== undefined && (
              <div className="flex items-center gap-1 text-sm text-muted-foreground">
                <Clock className="h-3 w-3" />
                {service.latency}ms
              </div>
            )}
            <StatusBadge status={service.status} />
          </div>
        </div>
      ))}
    </div>
  );
}
