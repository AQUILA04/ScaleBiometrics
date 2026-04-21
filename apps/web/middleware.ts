import { auth } from "@/lib/auth";
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Route protection middleware.
 * - Public routes: /login, /api/auth/*, /api/health
 * - All other routes require authentication → redirect to /login
 */

const PUBLIC_PATHS = [
  "/login",
  "/api/auth",
  "/api/health",
  "/_next",
  "/favicon.ico",
];

export default auth((request: NextRequest & { auth: unknown }) => {
  const { pathname } = request.nextUrl;

  // Allow public paths
  const isPublic = PUBLIC_PATHS.some((path) => pathname.startsWith(path));
  if (isPublic) return NextResponse.next();

  // Redirect unauthenticated users to login
  if (!request.auth) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("callbackUrl", request.url);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
});

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
