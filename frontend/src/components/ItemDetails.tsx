import { useEffect, useRef, useState } from 'react'
import type { MouseEvent } from 'react'
import { ArrowDown, ArrowUp, ArrowUpRight, Trash2, X } from 'lucide-react'
import type { PricePoint, WatchedItem } from '../types'
import { dateTime, money, shortDate } from '../format'
import PriceChart from './PriceChart'
import './ItemDetails.css'

type Props = {
    item: WatchedItem
    onClose: () => void
    onDelete: (id: number) => void
}

type PriceChange = {
    point: PricePoint
    delta: number | null
}

function priceChanges(points: PricePoint[]): PriceChange[] {
    const changes: PriceChange[] = []
    points.forEach((point, i) => {
        if (i === 0) {
            changes.push({ point, delta: null })
        } else if (point.price !== points[i - 1].price) {
            changes.push({ point, delta: point.price - points[i - 1].price })
        }
    })
    return changes.reverse()
}

function ItemDetails({ item, onClose, onDelete }: Props) {
    const dialogRef = useRef<HTMLDialogElement>(null)
    const [points, setPoints] = useState<PricePoint[] | null>(null)
    const [failed, setFailed] = useState(false)

    useEffect(() => {
        const dialog = dialogRef.current
        dialog?.showModal()
        // Focus the dialog itself so the first link doesn't open with a focus ring.
        dialog?.focus()
    }, [])

    useEffect(() => {
        fetch(`/items/${item.id}/history`)
            .then((res) => {
                if (!res.ok) throw new Error(`History request failed with ${res.status}`)
                return res.json()
            })
            .then(setPoints)
            .catch(() => setFailed(true))
    }, [item.id])

    function close() {
        dialogRef.current?.close()
    }

    function handleBackdropClick(event: MouseEvent<HTMLDialogElement>) {
        if (event.target === event.currentTarget) close()
    }

    function handleDelete() {
        onDelete(item.id)
        close()
    }

    const lowest = points?.length ? points.reduce((a, b) => (b.price < a.price ? b : a)) : null
    const highest = points?.length ? points.reduce((a, b) => (b.price > a.price ? b : a)) : null
    const changes = points ? priceChanges(points) : []

    return (
        <dialog
            ref={dialogRef}
            className="details"
            tabIndex={-1}
            aria-labelledby="details-title"
            onClose={onClose}
            onClick={handleBackdropClick}
        >
            <div className="details__body">
                <header className="details__header">
                    <div>
                        <h2 id="details-title" className="details__title">{item.name}</h2>
                        <div className="details__meta">
                            <code>{item.storeDomain}</code>
                            <a href={item.productUrl} target="_blank" rel="noreferrer">
                                View product <ArrowUpRight size={14} />
                            </a>
                        </div>
                    </div>
                    <button className="details__close" onClick={close} aria-label="Close">
                        <X size={20} />
                    </button>
                </header>

                <div className="details__stats">
                    <div className="details__stat">
                        <span className="details__label">Current</span>
                        <span className="details__value">
                            {item.lastPrice === null ? '—' : money.format(item.lastPrice)}
                        </span>
                    </div>
                    <div className="details__stat">
                        <span className="details__label">Target</span>
                        <span className="details__value details__value--accent">{money.format(item.targetPrice)}</span>
                    </div>
                    <div className="details__stat">
                        <span className="details__label">Lowest seen</span>
                        <span className="details__value">{lowest ? money.format(lowest.price) : '—'}</span>
                        {lowest && <span className="details__sub">{shortDate.format(new Date(lowest.checkedAt))}</span>}
                    </div>
                    <div className="details__stat">
                        <span className="details__label">Highest seen</span>
                        <span className="details__value">{highest ? money.format(highest.price) : '—'}</span>
                        {highest && <span className="details__sub">{shortDate.format(new Date(highest.checkedAt))}</span>}
                    </div>
                </div>

                <section className="details__section">
                    <div className="details__section-heading">
                        <h3>Price history</h3>
                        {points && points.length > 0 && (
                            <span className="details__sub">
                                {points.length} checks since {shortDate.format(new Date(points[0].checkedAt))}
                            </span>
                        )}
                    </div>
                    {failed && <p className="details__empty">Couldn't load price history.</p>}
                    {!failed && points === null && <p className="details__empty">Loading...</p>}
                    {points && points.length < 2 && (
                        <p className="details__empty">The chart appears after a couple of price checks.</p>
                    )}
                    {points && points.length >= 2 && <PriceChart points={points} targetPrice={item.targetPrice} />}
                </section>

                {changes.length > 0 && (
                    <section className="details__section">
                        <h3>Price changes</h3>
                        <ul className="details__changes">
                            {changes.slice(0, 10).map(({ point, delta }) => (
                                <li key={point.checkedAt}>
                                    <span className="details__change-date">{dateTime.format(new Date(point.checkedAt))}</span>
                                    <span className="details__change-price">{money.format(point.price)}</span>
                                    {delta === null ? (
                                        <span className="details__delta">Started tracking</span>
                                    ) : (
                                        <span className={`details__delta ${delta < 0 ? 'details__delta--down' : ''}`}>
                                            {delta < 0 ? <ArrowDown size={12} /> : <ArrowUp size={12} />}
                                            {money.format(Math.abs(delta))}
                                        </span>
                                    )}
                                </li>
                            ))}
                        </ul>
                        {changes.length === 1 && (
                            <p className="details__sub">No price changes since tracking started.</p>
                        )}
                    </section>
                )}

                <footer className="details__footer">
                    <button className="details__delete" onClick={handleDelete}>
                        <Trash2 size={16} /> Stop watching
                    </button>
                    {item.alerted && (
                        <a className="details__buy" href={item.productUrl} target="_blank" rel="noreferrer">
                            Purchase
                        </a>
                    )}
                </footer>
            </div>
        </dialog>
    )
}

export default ItemDetails
