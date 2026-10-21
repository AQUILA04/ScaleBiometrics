import NextAuth from "next-auth";
<<<<<<< HEAD
import type { NextAuthConfig } from "next-auth";
import Keycloak from "next-auth/providers/keycloak";

=======
import Keycloak from "next-auth/providers/keycloak";
import type { NextAuthConfig } from "next-auth";

/**
 * NextAuth.js v5 configuration for ScaleBiometrics.
 * Uses Keycloak as the sole OIDC provider with PKCE S256.
 * Realm: scalebiometrics | Client: scalebiometrics-web (public)
 */
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
export const authConfig: NextAuthConfig = {
  providers: [
    Keycloak({
      clientId: process.env.KEYCLOAK_CLIENT_ID ?? "scalebiometrics-web",
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET ?? "",
<<<<<<< HEAD
      issuer: process.env.KEYCLOAK_ISSUER ?? "http://localhost:8180/realms/scalebiometrics",
    }),
  ],
  callbacks: {
    async jwt({ token, account, profile }) {
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.idToken = account.id_token;
        (token as { role?: string }).role = (profile as { role?: string })?.role ?? "tenant_admin";
        (token as { tenantId?: string | null }).tenantId = (profile as { tenantId?: string | null })?.tenantId ?? null;
=======
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
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
      }
      return token;
    },
    async session({ session, token }) {
<<<<<<< HEAD
      const extSession = session as typeof session & {
        accessToken?: string;
        refreshToken?: string;
        idToken?: string;
        user: typeof session.user & { role: string; tenantId: string | null };
      };
      extSession.accessToken = token.accessToken as string;
      extSession.refreshToken = token.refreshToken as string;
      extSession.idToken = token.idToken as string;
      extSession.user.role = (token as { role?: string }).role as string;
      extSession.user.tenantId = (token as { tenantId?: string | null }).tenantId as string | null;
      return extSession;
=======
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
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
    },
  },
  pages: {
    signIn: "/login",
<<<<<<< HEAD
  },
  session: {
    strategy: "jwt",
=======
    error: "/login",
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
  },
};

export const { handlers, auth, signIn, signOut } = NextAuth(authConfig);
