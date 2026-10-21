<<<<<<< HEAD
// eslint-disable-next-line @typescript-eslint/no-unused-vars
type RequestOptions<TResponse> = {
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  body?: unknown;
  headers?: Record<string, string>;
};

class ApiError extends Error {
  constructor(
    public status: number,
    message: string
=======
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
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
  ) {
    super(message);
    this.name = "ApiError";
  }
}

<<<<<<< HEAD
export async function apiClient<TResponse>(
  path: string,
  options: RequestOptions<TResponse> = {}
): Promise<TResponse> {
  const { method = "GET", body, headers = {} } = options;

  const response = await fetch(`/api/proxy/${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
    credentials: "include",
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: "Request failed" }));
    throw new ApiError(response.status, error.message);
  }

  if (response.status === 204) {
    return undefined as TResponse;
  }

  return response.json();
}

export const api = {
  get: <TResponse>(path: string) => apiClient<TResponse>(path),
  post: <TResponse>(path: string, body: unknown) =>
    apiClient<TResponse>(path, { method: "POST", body }),
  put: <TResponse>(path: string, body: unknown) =>
    apiClient<TResponse>(path, { method: "PUT", body }),
  delete: <TResponse>(path: string) =>
    apiClient<TResponse>(path, { method: "DELETE" }),
  patch: <TResponse>(path: string, body: unknown) =>
    apiClient<TResponse>(path, { method: "PATCH", body }),
};
=======
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
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
