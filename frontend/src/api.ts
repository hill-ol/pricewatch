import type { NewItem, PricePoint, WatchedItem } from './types'

// Every call to the backend goes through here. In dev, Vite proxies /items to the Spring Boot app.

export class ApiError extends Error {
    readonly status: number

    constructor(method: string, path: string, status: number) {
        super(`${method} ${path} failed with ${status}`)
        this.status = status
    }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const res = await fetch(path, {
        ...init,
        headers: init.body ? { 'Content-Type': 'application/json', ...init.headers } : init.headers,
    })
    if (!res.ok) {
        throw new ApiError(init.method ?? 'GET', path, res.status)
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
