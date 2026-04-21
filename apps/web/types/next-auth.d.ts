import type { DefaultSession, DefaultJWT } from "next-auth";
import type { UserRole } from "./index";

declare module "next-auth" {
  interface Session extends DefaultSession {
    user: {
      tenantId?: string;
      roles: UserRole[];
    } & DefaultSession["user"];
    accessToken?: string;
  }
}

declare module "next-auth/jwt" {
  interface JWT extends DefaultJWT {
    accessToken?: string;
    refreshToken?: string;
    expiresAt?: number;
    tenantId?: string;
    roles?: UserRole[];
  }
}
