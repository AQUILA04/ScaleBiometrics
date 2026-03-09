---
name: scalebiometrics-components-build
description: >
  Guidelines and specifications for building UI components for the ScaleBiometrics project.
  Includes design tokens, styling rules, component inventory, and accessibility requirements.
license: MIT
---

# ScaleBiometrics UI Component Guidelines

This skill provides the comprehensive design system and component building rules for the ScaleBiometrics frontend. It is intended to guide agents in creating consistent, accessible, and performant UI components using Next.js, Tailwind CSS, and Shadcn/UI.

## Table of Contents
1. [Design Tokens](#design-tokens)
2. [Styling Rules](#styling-rules)
3. [Components](#components)
4. [Accessibility](#accessibility)

## Quick Reference

### Core Layout Pattern
The application uses a responsive layout with a top header, primary sidebar (icons), secondary sidebar (contextual), and a flexible content area. On mobile, sidebars collapse into a drawer.

### Tailwind Config Highlights
```typescript
// tailwind.config.ts snippet
module.exports = {
  theme: {
    extend: {
      colors: {
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        // ... other semantic colors
      },
      fontFamily: {
        sans: ["var(--font-geist-sans)", "sans-serif"],
        mono: ["var(--font-geist-mono)", "monospace"],
      },
    },
  },
}
```

### CSS Variables
```css
@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --card: 210 40% 98%;
    --card-foreground: 222.2 84% 4.9%;
    --primary: 221.2 83.2% 53.3%;
    --primary-foreground: 210 40% 98%;
    /* ... */
  }
  .dark {
    --background: 222.2 84% 4.9%;
    --foreground: 210 40% 98%;
    --card: 217.2 32.6% 17.5%;
    --card-foreground: 210 40% 98%;
    --primary: 217.2 91.2% 59.8%;
    --primary-foreground: 222.2 47.4% 11.2%;
    /* ... */
  }
}
```
