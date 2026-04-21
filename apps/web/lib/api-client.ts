/**
 * Typed fetch wrapper for BFF proxy calls.
 * All requests go through Next.js API routes which attach the Bearer JWT.
 */

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const PROXY_BASE = "/api/proxy";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public data?: unknown
  ) {
    super(message);
    this.name = "ApiError";
  }
}

interface FetchOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>;
}

async function fetchJson<T>(url: string, options: FetchOptions = {}): Promise<T> {
  const { params, ...init } = options;

  let fullUrl = url;
  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined) searchParams.set(key, String(value));
    });
    const qs = searchParams.toString();
    if (qs) fullUrl = `${url}?${qs}`;
  }

  const response = await fetch(fullUrl, {
    headers: {
      "Content-Type": "application/json",
      ...init.headers,
    },
    ...init,
  });

  if (!response.ok) {
    let errorData: unknown;
    try {
      errorData = await response.json();
    } catch {
      errorData = await response.text();
    }
    throw new ApiError(response.status, `HTTP ${response.status}`, errorData);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

/**
 * Client-side API calls — go through BFF proxy.
 */
export const apiClient = {
  get: <T>(path: string, options?: FetchOptions) =>
    fetchJson<T>(`${PROXY_BASE}${path}`, { method: "GET", ...options }),

  post: <T>(path: string, body?: unknown, options?: FetchOptions) =>
    fetchJson<T>(`${PROXY_BASE}${path}`, {
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    }),

  put: <T>(path: string, body?: unknown, options?: FetchOptions) =>
    fetchJson<T>(`${PROXY_BASE}${path}`, {
      method: "PUT",
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    }),

  delete: <T>(path: string, options?: FetchOptions) =>
    fetchJson<T>(`${PROXY_BASE}${path}`, { method: "DELETE", ...options }),
};

/**
 * Server-side API calls — direct to Java backend (used in API routes).
 */
export const serverApiClient = {
  get: <T>(path: string, token: string, options?: FetchOptions) =>
    fetchJson<T>(`${API_BASE}${path}`, {
      method: "GET",
      headers: { Authorization: `Bearer ${token}` },
      ...options,
    }),

  post: <T>(path: string, token: string, body?: unknown, options?: FetchOptions) =>
    fetchJson<T>(`${API_BASE}${path}`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    }),
};

export { API_BASE };
