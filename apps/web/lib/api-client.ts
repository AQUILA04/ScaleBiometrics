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
  ) {
    super(message);
    this.name = "ApiError";
  }
}

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
