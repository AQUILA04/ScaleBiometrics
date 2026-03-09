# ScaleBiometrics Components Build Skill

This directory contains the `scalebiometrics-components-build` skill. It is generated automatically from the visual specifications and mockups provided for the ScaleBiometrics project.

## Purpose
The purpose of this skill is to provide agents with a comprehensive, project-specific guide for building UI components. It ensures that any frontend code generated adheres to the defined design system, styling rules, and accessibility standards of the ScaleBiometrics platform.

## Structure
- `SKILL.md`: The main entry point and metadata for the skill.
- `rules/`: Directory containing specific rule categories:
  - `design-tokens.md`: Color palette, typography, spacing, and shadows.
  - `styling.md`: Guidelines for using Tailwind CSS, `cn` utility, and CVA.
  - `components.md`: Inventory and code structure for key components.
  - `accessibility.md`: WCAG 2.1 AA compliance rules.
- `AGENTS.md`: A compiled version of all rules for easy agent consumption.

## Usage
Agents should read `AGENTS.md` before starting any UI development tasks for the ScaleBiometrics project.
