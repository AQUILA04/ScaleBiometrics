# Accessibility Rules

ScaleBiometrics targets WCAG 2.1 AA compliance.

## Minimum Touch Targets
All interactive elements (buttons, links, checkboxes, radio buttons, etc.) must have a minimum touch target size of **44x44px** to be easily tappable, especially on mobile devices.

```tsx
// Example of ensuring a button has a minimum height and padding
<Button className="min-h-[44px] px-4 py-2">
  Action
</Button>
```

## Form Labels and Association
Every input must have an associated `<Label>`. Use the `htmlFor` attribute on the label and the `id` attribute on the input to link them explicitly.

```tsx
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"

// ...
<div className="grid w-full max-w-sm items-center gap-1.5">
  <Label htmlFor="email">Email</Label>
  <Input type="email" id="email" placeholder="Email" />
</div>
```

## ARIA Attributes
Use ARIA attributes to enhance semantics when native HTML elements are insufficient.

- **`aria-label`**: Use for icon-only buttons or elements where the visual label is missing.
  ```tsx
  <Button variant="ghost" size="icon" aria-label="Close dialog">
    <X className="h-4 w-4" />
  </Button>
  ```
- **`aria-describedby`**: Link inputs to their description or error messages.
  ```tsx
  <Input id="password" type="password" aria-describedby="password-error" />
  <p id="password-error" className="text-sm text-destructive">Password is required.</p>
  ```
- **`aria-live`**: Use for dynamic content updates (like toasts or live queue updates).
  ```tsx
  <div aria-live="polite">
    {queueCount} jobs in queue
  </div>
  ```

## Focus Management
- Ensure visible focus indicators are never removed (`outline-none` should be paired with a custom focus ring, e.g., `focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2`).
- When a modal or dialog opens, focus must be trapped within it. Shadcn's `<Dialog>` component handles this automatically.
- Ensure logical tab order through the application.

## Contrast
Ensure text has sufficient contrast against its background.
- Normal text: Minimum 4.5:1
- Large text (≥18px) or UI components: Minimum 3:1
Use the predefined semantic colors (e.g., `text-foreground` on `bg-background`) to maintain contrast automatically.
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
# Design Tokens

## Color Palette

### Main Colors
- **Background**: Light `#FFFFFF`, Dark `#020817`
- **Foreground**: Light `#020817`, Dark `#F8FAFC`
- **Card**: Light `#F8FAFC`, Dark `#0F172A`
- **Border**: Light `#E2E8F0`, Dark `#1E293B`
- **Muted**: Light `#F1F5F9`, Dark `#1E293B`
- **Muted Foreground**: Light `#64748B`, Dark `#94A3B8`

### Semantic Colors
- **Primary**: Light `#2563EB`, Dark `#3B82F6` (Buttons, Links, Focus)
- **Primary Foreground**: `#FFFFFF`
- **Secondary**: Light `#475569`, Dark `#94A3B8`
- **Destructive**: Light `#EF4444`, Dark `#F87171` (Delete actions, Errors)
- **Success**: Light `#22C55E`, Dark `#4ADE80` (Active/Healthy status)
- **Warning**: Light `#F59E0B`, Dark `#FBBF24` (Pending/Warning status)
- **Info**: Light `#3B82F6`, Dark `#60A5FA` (Processing status, Tooltips)

## Typography

- **Font Families**: Geist Sans (sans-serif), Geist Mono (monospace)
- **H1**: Bold (700), 24px (Mobile) / 30px (Desktop), Line Height 1.2
- **H2**: SemiBold (600), 20px (Mobile) / 24px (Desktop), Line Height 1.3
- **H3**: SemiBold (600), 18px (Mobile) / 20px (Desktop), Line Height 1.4
- **H4**: Medium (500), 16px (Mobile) / 18px (Desktop), Line Height 1.4
- **Body**: Regular (400), 14px (Mobile) / 16px (Desktop), Line Height 1.5
- **Body Small**: Regular (400), 12px (Mobile) / 14px (Desktop), Line Height 1.5
- **Code**: Regular (400), 13px (Mobile) / 14px (Desktop), Line Height 1.5

## Spacing
- `xs`: 4px
- `sm`: 8px
- `md`: 16px
- `lg`: 24px
- `xl`: 32px
- `2xl`: 48px

## Shadows & Elevation
- **Level 1**: `shadow-sm` (Inputs, borders)
- **Level 2**: `shadow-md` (Cards, panels)
- **Level 3**: `shadow-lg` (Dropdowns, popovers)
- **Level 4**: `shadow-xl` (Modals, dialogs)

## Border Radius
- **Small**: `4px` (`rounded`)
- **Medium**: `8px` (`rounded-lg`)
- **Large**: `12px` (`rounded-xl`)
- **Full**: `9999px` (`rounded-full`)
# Styling Rules

## Utility Function (`cn`)
We use a standard utility function `cn` to merge Tailwind classes, resolving conflicts efficiently.

```typescript
import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

## Component Variants (`cva`)
For components with multiple visual states (e.g., Buttons, Badges), use `class-variance-authority` (CVA) to define variants cleanly.

```typescript
import { cva, type VariantProps } from "class-variance-authority"

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-primary text-primary-foreground hover:bg-primary/80",
        secondary:
          "border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80",
        destructive:
          "border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80",
        outline: "text-foreground",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)
```

## Responsive Design
- Follow a **mobile-first** approach. Use un-prefixed utility classes for mobile styling and apply responsive prefixes (`sm:`, `md:`, `lg:`, `xl:`, `2xl:`) for larger screens.
- Avoid ad-hoc styling; ensure responsiveness follows the specified design tokens.

## Specific Styling Rules
- **Monospace Fonts**: Use `font-mono` for IDs, code snippets, logs, and exact numeric data (e.g., Tenant IDs, API Keys).
- **Dark Mode**: Support dark mode natively using Tailwind's `dark:` variant or CSS variables.
