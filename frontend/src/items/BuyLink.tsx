import './BuyLink.css'

type Props = {
    url: string
    className?: string
}

// Primary "Purchase" call to action, shown wherever an item has hit its target price.
// Stops click propagation so it can sit inside clickable containers like ItemCard.
function BuyLink({ url, className = '' }: Props) {
    return (
        <a
            className={`buy-link ${className}`.trim()}
            href={url}
            target="_blank"
            rel="noreferrer"
            onClick={(event) => event.stopPropagation()}
        >
            Purchase
        </a>
    )
}

export default BuyLink
