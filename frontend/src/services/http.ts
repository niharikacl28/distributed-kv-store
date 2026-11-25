/**
 * Minimal REST client for the distributed-kv-store backend.
 *
 * Requests are issued relative to the current origin by default. During
 * development the Vite dev server proxies /api to http://localhost:8080
 * (see vite.config.ts), which keeps the browser same-origin and avoids CORS.
 * Set VITE_API_BASE_URL to target a different backend origin in production.
 */

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export class HttpError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'HttpError'
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  })

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`

    try {
      const body = (await response.json()) as { error?: string }
      if (body?.error) {
        message = body.error
      }
    } catch {
      // Response had no JSON body; keep the default message.
    }

    throw new HttpError(message, response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export interface ApiErrorInfo {
  /** HTTP status, or null when the request never reached the backend. */
  status: number | null
  /** True for a network/CORS failure rather than an HTTP error response. */
  disconnected: boolean
  message: string
}

/** Normalises any thrown value from the API layer into a renderable shape. */
export function toApiError(error: unknown): ApiErrorInfo {
  if (error instanceof HttpError) {
    return { status: error.status, disconnected: false, message: error.message }
  }
  if (error instanceof TypeError) {
    return {
      status: null,
      disconnected: true,
      message: 'Backend unavailable. The request could not reach the server.',
    }
  }
  return {
    status: null,
    disconnected: false,
    message:
      error instanceof Error ? error.message : 'Unexpected client error',
  }
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
