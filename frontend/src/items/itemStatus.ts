import type { WatchedItem } from '../types'

// 'at-target' is based on the price itself, not on whether the alert email went out:
// `alerted` only flips to true after a successful send, so a failed email would
// otherwise hide a live deal.
export type ItemStatus = 'pending' | 'at-target' | 'watching'

export function itemStatus(item: WatchedItem): ItemStatus {
    if (item.lastPrice === null) return 'pending'
    return item.lastPrice <= item.targetPrice ? 'at-target' : 'watching'
}

export function isAtTarget(item: WatchedItem): boolean {
    return itemStatus(item) === 'at-target'
}
