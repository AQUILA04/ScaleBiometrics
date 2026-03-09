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
