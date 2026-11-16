import { useState } from 'react'
import type { MouseEvent } from 'react'
import type { WatchedItem } from '../types'
import { money } from '../format'
import { itemStatus } from '../itemStatus'
import BuyLink from './BuyLink'
import './ItemCard.css'
import { ArrowDown, ArrowUpRight, ChevronRight, Trash2 } from 'lucide-react'

type Props = {
    item: WatchedItem
    onOpen: (id: number) => void
    onDelete: (id: number) => void
}

function stop(event: MouseEvent) {
    event.stopPropagation()
}

function ItemCard({ item, onOpen, onDelete }: Props) {
    const [confirmingRemove, setConfirmingRemove] = useState(false)
    const { lastPrice, targetPrice } = item
    const status = itemStatus(item)
    const variantNumber = item.variantId.split('/').pop()

    let badgeText = 'Pending first check'
    if (status === 'at-target') badgeText = item.alerted ? 'Alert sent' : 'At target · email not sent'
    if (status === 'watching' && lastPrice !== null) {
        badgeText = `Watching (needs ${money.format(lastPrice - targetPrice)} drop)`
    }

    return (
        <article className={`item-card item-card--${status}`} onClick={() => onOpen(item.id)}>
            <div className="item-card__top">
                <span className={`badge badge--${status}`}>{badgeText}</span>
                {!confirmingRemove && (
                    <button
                        className="item-card__delete"
                        onClick={(event) => {
                            stop(event)
                            setConfirmingRemove(true)
                        }}
                        aria-label={`Remove ${item.name}`}
                        title="Remove"
                    >
                        <Trash2 size={16} />
                    </button>
                )}
            </div>

            {confirmingRemove && (
                <div
                    className="item-card__confirm"
                    role="alertdialog"
                    aria-label={`Confirm removing ${item.name}`}
                    onClick={stop}
                    onKeyDown={(event) => {
                        if (event.key === 'Escape') setConfirmingRemove(false)
                    }}
                >
                    <span>Remove this item? Its price history will be deleted too.</span>
                    <div className="item-card__confirm-actions">
                        <button className="item-card__confirm-cancel" onClick={() => setConfirmingRemove(false)} autoFocus>
                            Cancel
                        </button>
                        <button className="item-card__confirm-remove" onClick={() => onDelete(item.id)}>
                            Remove
                        </button>
                    </div>
                </div>
            )}

            <a className="item-card__name" href={item.productUrl} target="_blank" rel="noreferrer" onClick={stop}>
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
                    {status === 'at-target' && lastPrice !== null && (
                        <span className="item-card__drop">
                            {lastPrice < targetPrice ? (
                                <>
                                    <ArrowDown size={12} />
                                    {money.format(targetPrice - lastPrice)} below target
                                </>
                            ) : (
                                'At your target'
                            )}
                        </span>
                    )}
                </div>

                <div className="item-card__price">
                    <span className="item-card__label">Your target</span>
                    <span className="item-card__value item-card__value--target">{money.format(targetPrice)}</span>
                </div>

                {status === 'at-target' && (
                    <BuyLink url={item.productUrl} className="item-card__buy" />
                )}
            </div>

            <button
                className="item-card__details"
                onClick={(event) => {
                    stop(event)
                    onOpen(item.id)
                }}
            >
                Price history
                <ChevronRight size={16} />
            </button>
        </article>
    )
}

export default ItemCard
