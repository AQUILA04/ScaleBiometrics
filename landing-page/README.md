# BioEngine landing page (static)

Public marketing site for **BioEngine** (slug `scalebio`).

- Host: `https://scalebio.optimizesolux.com/` (Traefik `Path(/)` only)
- Contact: `contact.bioengine@optimizesolux.com`
- Local preview: `npx --yes serve .` from this folder

## Contabo

Path on VPS: `/opt/optimizesolux/scalebio-landing`  
Compose project: `scalebio-landing`  
Network: external `traefik-public`

Deploy via `.github/workflows/deploy-landing-page.yml` (push `landing-page/**` on `main` or `workflow_dispatch`).

## Email routing (manual)

Configure Cloudflare Email Routing for `contact.bioengine@optimizesolux.com` (and optional `bioengine@`) → forward destination.
