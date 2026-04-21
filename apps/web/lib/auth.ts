import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";
import type { NextAuthConfig } from "next-auth";

/**
 * NextAuth.js v5 configuration for ScaleBiometrics.
 * Uses Keycloak as the sole OIDC provider with PKCE S256.
 * Realm: scalebiometrics | Client: scalebiometrics-web (public)
 */
export const authConfig: NextAuthConfig = {
  providers: [
    Keycloak({
      clientId: process.env.KEYCLOAK_CLIENT_ID ?? "scalebiometrics-web",
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET ?? "",
      issuer: `${process.env.KEYCLOAK_URL ?? "http://localhost:8080"}/realms/${
        process.env.KEYCLOAK_REALM ?? "scalebiometrics"
      }`,
    }),
  ],
  session: {
    strategy: "jwt",
  },
  callbacks: {
    async jwt({ token, account, profile }) {
      // Persist Keycloak access token and custom claims
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.expiresAt = account.expires_at;
      }
      // Extract custom claims from Keycloak token
      if (profile) {
        const p = profile as Record<string, unknown>;
        token.tenantId = p["tenant_id"] as string | undefined;
        token.roles = (p["roles"] as string[]) ?? [];
      }
      return token;
    },
    async session({ session, token }) {
      // Expose safe claims to the client session
      return {
        ...session,
        user: {
          ...session.user,
          tenantId: token.tenantId as string | undefined,
          roles: token.roles as string[],
        },
        accessToken: token.accessToken as string | undefined,
      };
    },
  },
  pages: {
    signIn: "/login",
    error: "/login",
  },
};

export const { handlers, auth, signIn, signOut } = NextAuth(authConfig);
