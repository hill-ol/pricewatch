import { ArrowUpRight } from 'lucide-react'
import './ProductLink.css'

type Props = {
    url: string
    name: string
    className?: string
}

// Item name that links out to the product page, used as the title on the card and in the details modal.
// Stops click propagation so it can sit inside clickable containers like ItemCard.
function ProductLink({ url, name, className = '' }: Props) {
    return (
        <a
            className={`product-link ${className}`.trim()}
            href={url}
            target="_blank"
            rel="noreferrer"
            onClick={(event) => event.stopPropagation()}
        >
            {name}
            <ArrowUpRight size={16} className="product-link__icon" />
        </a>
    )
}

export default ProductLink
