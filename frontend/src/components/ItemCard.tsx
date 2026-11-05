import type { WatchedItem } from '../types'
import './ItemCard.css'
import {ArrowDown, ArrowUpRight, Trash2} from "lucide-react";

type Props = {
    item: WatchedItem
    onDelete: (id: number) => void
}

const money = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
})

function ItemCard({ item, onDelete }: Props) {
    const { lastPrice, targetPrice } = item
    const status = lastPrice === null ? 'pending' : item.alerted ? 'alerted' : 'watching'
    const variantNumber = item.variantId.split('/').pop()

    let badgeText = 'Pending first check'
    if (status === 'alerted') badgeText = 'Alert sent'
    if (status === 'watching' && lastPrice !== null) {
        badgeText = `Watching (needs ${money.format(lastPrice - targetPrice)} drop)`
    }

    return (
        <article className={`item-card item-card--${status}`}>
            <div className="item-card__top">
                <span className={`badge badge--${status}`}>{badgeText}</span>
                <button
                    className="item-card__delete"
                    onClick={() => onDelete(item.id)}
                    aria-label={`Stop watching ${item.name}`}
                >
                    <Trash2 size={16} />
                </button>
            </div>

            <a className="item-card__name" href={item.productUrl} target="_blank" rel="noreferrer">
                {item.name}
                <ArrowUpRight size={16} />
            </a>

            <div className="item-card__meta">
                <code>{item.storeDomain}</code>
                <span>Variant #{variantNumber}</span>
            </div>

            <div className="item-card__prices">
                <div className="item-card__price">
                    <span className="item-card__label">Current price</span>
                    {lastPrice === null ? (
                        <span className="item-card__value item-card__value--empty">Not checked yet</span>
                    ) : (
                        <span className="item-card__value">{money.format(lastPrice)}</span>
                    )}
                    {status === 'alerted' && lastPrice !== null && (
                        <span className="item-card__drop">
                            <ArrowDown size={12} />
                            {money.format(targetPrice - lastPrice)} below target
                        </span>
                    )}
                </div>

                <div className="item-card__price">
                    <span className="item-card__label">Your target</span>
                    <span className="item-card__value item-card__value--target">{money.format(targetPrice)}</span>
                </div>

                {status === 'alerted' && (
                    <a className="item-card__buy" href={item.productUrl} target="_blank" rel="noreferrer">
                        Buy on Shopify
                    </a>
                )}
            </div>
        </article>
    )
}

export default ItemCard