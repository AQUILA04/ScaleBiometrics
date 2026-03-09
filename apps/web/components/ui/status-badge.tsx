import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const statusBadgeVariants = cva(
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold transition-colors",
  {
    variants: {
      variant: {
        default: "bg-primary/10 text-primary",
        active: "bg-green-500/10 text-green-500",
        suspended: "bg-yellow-500/10 text-yellow-500",
        pending: "bg-blue-500/10 text-blue-500",
        deleted: "bg-red-500/10 text-red-500",
        error: "bg-red-500/10 text-red-500",
        processing: "bg-blue-500/10 text-blue-500",
        healthy: "bg-green-500/10 text-green-500",
        degraded: "bg-yellow-500/10 text-yellow-500",
        down: "bg-red-500/10 text-red-500",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

export interface StatusBadgeProps
  extends VariantProps<typeof statusBadgeVariants> {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const normalizedStatus = status.toLowerCase() as StatusBadgeProps["variant"];
  
  return (
    <span className={cn(statusBadgeVariants({ variant: normalizedStatus }), className)}>
      {status}
    </span>
  );
}
