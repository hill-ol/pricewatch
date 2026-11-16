import type { NewItem, PricePoint, WatchedItem } from './types'

// Every call to the backend goes through here. In dev, Vite proxies /items to the Spring Boot app.

// The backend's RFC 9457 error body (see ApiExceptionHandler). Validation errors add `errors`.
type Problem = {
    detail?: string
    errors?: Record<string, string>
}

export class ApiError extends Error {
    readonly status: number
    /** Human-readable reason from the server, when it sent one. */
    readonly detail: string | null
    /** Field name → message, for validation errors. */
    readonly fieldErrors: Record<string, string>

    constructor(method: string, path: string, status: number, problem: Problem = {}) {
        super(problem.detail ?? `${method} ${path} failed with ${status}`)
        this.status = status
        this.detail = problem.detail ?? null
        this.fieldErrors = problem.errors ?? {}
    }
}

async function readProblem(res: Response): Promise<Problem> {
    if (!res.headers.get('Content-Type')?.includes('json')) return {}
    try {
        return await res.json()
    } catch {
        return {}
    }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const res = await fetch(path, {
        ...init,
        headers: init.body ? { 'Content-Type': 'application/json', ...init.headers } : init.headers,
    })
    if (!res.ok) {
        throw new ApiError(init.method ?? 'GET', path, res.status, await readProblem(res))
    }
    // 204 No Content (DELETE) has no body to parse.
    return (res.status === 204 ? undefined : await res.json()) as T
}

export function getItems(): Promise<WatchedItem[]> {
    return request('/items')
}

export function createItem(item: NewItem): Promise<WatchedItem> {
    return request('/items', { method: 'POST', body: JSON.stringify(item) })
}

export function updateTargetPrice(id: number, targetPrice: number): Promise<WatchedItem> {
    return request(`/items/${id}`, { method: 'PATCH', body: JSON.stringify({ targetPrice }) })
}

export function deleteItem(id: number): Promise<void> {
    return request(`/items/${id}`, { method: 'DELETE' })
}

export function getPriceHistory(id: number): Promise<PricePoint[]> {
    return request(`/items/${id}/history`)
}

/**
 * A message to show the user for a failed request: each invalid field (using `fieldLabels`
 * for friendly names), otherwise the server's explanation, otherwise `fallback`.
 * Network failures and errors without a body fall back.
 */
export function describeError(
    error: unknown,
    fallback: string,
    fieldLabels: Record<string, string> = {},
): string {
    if (!(error instanceof ApiError)) return fallback
    const fields = Object.entries(error.fieldErrors)
    if (fields.length > 0) {
        return fields.map(([field, message]) => `${fieldLabels[field] ?? field} ${message}`).join('\n')
    }
    return error.detail ?? fallback
}
