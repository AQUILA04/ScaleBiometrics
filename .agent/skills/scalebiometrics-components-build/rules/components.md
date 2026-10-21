# Components

This section lists the key components identified for the ScaleBiometrics project, based on the provided mockups and specifications. All components should be built on top of Shadcn/UI primitives.

## Component Inventory

### 1. `MetricCard` (or `KpiCard`)
Displays key performance indicators (e.g., Pending Queue, Processed, Success Rate, Avg Latency).

**Code Structure:**
```tsx
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { cn } from "@/lib/utils"

interface MetricCardProps {
  title: string
  value: string | number
  trend?: {
    value: string
    isPositive: boolean
  }
  icon?: React.ReactNode
  className?: string
}

export function MetricCard({ title, value, trend, icon, className }: MetricCardProps) {
  return (
    <Card className={cn("bg-card border-border shadow-md", className)}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        {icon && <div className="text-muted-foreground">{icon}</div>}
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {trend && (
          <p className={cn(
            "text-xs mt-1",
            trend.isPositive ? "text-success" : "text-destructive"
          )}>
            {trend.isPositive ? "+" : ""}{trend.value}
          </p>
        )}
      </CardContent>
    </Card>
  )
}
```

### 2. `StatusBadge`
Displays the status of a tenant, job, or system health (e.g., Active, Pending, Error).

**Code Structure:**
```tsx
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

interface StatusBadgeProps {
  status: 'active' | 'pending' | 'error' | 'processing' | 'success' | 'warning'
  label?: string
  className?: string
}

export function StatusBadge({ status, label, className }: StatusBadgeProps) {
  const statusStyles = {
    active: "bg-success/10 text-success border-success/20",
    success: "bg-success/10 text-success border-success/20",
    pending: "bg-warning/10 text-warning border-warning/20",
    warning: "bg-warning/10 text-warning border-warning/20",
    error: "bg-destructive/10 text-destructive border-destructive/20",
    processing: "bg-info/10 text-info border-info/20",
  }

  const defaultLabels = {
    active: "Active",
    success: "Success",
    pending: "Pending",
    warning: "Warning",
    error: "Error",
    processing: "Processing",
  }

  return (
    <Badge variant="outline" className={cn(statusStyles[status], className)}>
      {label || defaultLabels[status]}
    </Badge>
  )
}
```

### 3. `AppLayout` (Main Application Layout)
The main layout wrapper for the SuperAdmin and Tenant consoles.

**Structure:**
- **Top Header**: Logo, Breadcrumb, Search, Notifications, Profile Menu.
- **Sidebar Navigation**: Primary (icons) and Secondary (contextual links).
- **Main Content Area**: Flexible width, padded content area.

### 4. `Wizard` (Multi-step Form)
Used for complex creations like Tenant Provisioning (`SA-3.2`).

**Structure:**
- **Stepper**: Horizontal (desktop) or vertical (mobile) progress indicator.
- **Step Content**: The form fields for the current step.
- **Navigation Buttons**: Previous, Next, Finish/Submit.

### 5. `DataTable`
Used for displaying lists like Tenants, Queue Jobs, API Keys, etc.

**Features:**
- Sortable columns.
- Pagination controls.
- Search/Filter bar integration.
- Row actions (Dropdown menu per row).
- Mass actions (when rows are selected).
