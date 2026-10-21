<<<<<<< HEAD
import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/lib/auth";

const API_GATEWAY_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export async function GET(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  return proxyRequest(request, params.path.join("/"), "GET");
}

export async function POST(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  return proxyRequest(request, params.path.join("/"), "POST");
}

export async function PUT(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  return proxyRequest(request, params.path.join("/"), "PUT");
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  return proxyRequest(request, params.path.join("/"), "DELETE");
}

export async function PATCH(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  return proxyRequest(request, params.path.join("/"), "PATCH");
}

async function proxyRequest(
  request: NextRequest,
  path: string,
  method: string
): Promise<NextResponse> {
  const session = await auth();

  const accessToken = (session as { accessToken?: string })?.accessToken;
  if (!accessToken) {
    return NextResponse.json(
      { message: "Unauthorized" },
      { status: 401 }
    );
  }

  const url = new URL(request.url);
  const queryString = url.search.toString();
  const targetUrl = `${API_GATEWAY_URL}/${path}${queryString ? `?${queryString}` : ""}`;

  const headers: Record<string, string> = {
    Authorization: `Bearer ${accessToken}`,
  };

  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers["Content-Type"] = contentType;
  }

  try {
    const response = await fetch(targetUrl, {
      method,
      headers,
      body: method !== "GET" && method !== "HEAD" ? await request.text() : undefined,
      credentials: "include",
    });

    const data = await response.text();

    return new NextResponse(data, {
      status: response.status,
      headers: {
        ...Object.fromEntries(
          Array.from(response.headers.entries()).filter(
            ([key]) => !["content-encoding", "transfer-encoding", "connection"].includes(key.toLowerCase())
          )
        ),
      },
    });
  } catch (error) {
    console.error("Proxy request failed:", error);
    return NextResponse.json(
      { message: "Gateway unavailable" },
      { status: 503 }
    );
  }
}
=======
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
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
