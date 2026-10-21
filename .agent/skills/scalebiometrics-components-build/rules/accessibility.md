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
