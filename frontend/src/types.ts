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

export type PricePoint = {
    price: number
    checkedAt: string
}

export type NewItem = {
    name: string
    productUrl: string
    storeDomain: string
    variantId: string
    targetPrice: number
}