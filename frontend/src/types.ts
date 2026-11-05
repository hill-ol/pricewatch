export type WatchedItem = {
    id: number
    name: string
    productUrl: string
    storeDomain: string
    variantId: string
    targetPrice: number
    lastPrice: number | null
    alerted: boolean
}