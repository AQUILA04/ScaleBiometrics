import NextAuth from "next-auth";
import type { NextAuthConfig } from "next-auth";
import Keycloak from "next-auth/providers/keycloak";

export const authConfig: NextAuthConfig = {
  providers: [
    Keycloak({
      clientId: process.env.KEYCLOAK_CLIENT_ID ?? "scalebiometrics-web",
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET ?? "",
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
      }
      return token;
    },
    async session({ session, token }) {
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
    },
  },
  pages: {
    signIn: "/login",
  },
  session: {
    strategy: "jwt",
  },
};

export const { handlers, auth, signIn, signOut } = NextAuth(authConfig);
