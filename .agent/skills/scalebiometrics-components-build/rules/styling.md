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
