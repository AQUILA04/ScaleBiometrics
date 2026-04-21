import { auth } from "@/lib/auth";
import { NextRequest, NextResponse } from "next/server";

const API_BASE = process.env.API_BASE_URL ?? "http://localhost:8080";

/**
 * BFF Proxy — forwards authenticated requests to the Java backend.
 * The access token is retrieved from the server-side session and
 * attached as a Bearer header. Tokens are NEVER exposed to client JS.
 */
async function handler(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const session = await auth();

  if (!session || !session.accessToken) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const path = params.path.join("/");
  const url = new URL(`/${path}`, API_BASE);

  // Forward query parameters
  request.nextUrl.searchParams.forEach((value, key) => {
    url.searchParams.set(key, value);
  });

  try {
    const response = await fetch(url.toString(), {
      method: request.method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${session.accessToken}`,
        "X-Tenant-ID": session.user?.tenantId ?? "",
        "X-Request-ID": crypto.randomUUID(),
      },
      body:
        request.method !== "GET" && request.method !== "HEAD"
          ? await request.text()
          : undefined,
    });

    const data = response.status !== 204 ? await response.json() : null;

    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error("[BFF Proxy] Error:", error);
    return NextResponse.json(
      { error: "Internal proxy error" },
      { status: 502 }
    );
  }
}

export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const PATCH = handler;
export const DELETE = handler;
