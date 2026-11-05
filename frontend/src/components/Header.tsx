import { Archive, BellRing, TrendingDown } from 'lucide-react'
import './Header.css'

type Props = {
    monitoredCount: number
    triggeredCount: number
}

function Header({ monitoredCount, triggeredCount }: Props) {
    return (
        <header>
            <div className="topbar">
        <span className="topbar__logo">
          <BellRing size={18} />
        </span>
                <span className="topbar__title">Pricewatch</span>
            </div>

            <div className="page-heading">
                <h1>Active Price Feeds</h1>
                <div className="summary">
                    <div className="summary__chip summary__chip--monitored">
                        <Archive size={18} />
                        <div>
                            <span className="summary__label">Monitored</span>
                            <span className="summary__value">
                {monitoredCount} {monitoredCount === 1 ? 'Item' : 'Items'}
              </span>
                        </div>
                    </div>
                    <div className="summary__chip summary__chip--triggered">
                        <TrendingDown size={18} />
                        <div>
                            <span className="summary__label">Triggered</span>
                            <span className="summary__value">
                {triggeredCount} {triggeredCount === 1 ? 'Price Drop' : 'Price Drops'}
              </span>
                        </div>
                    </div>
                </div>
            </div>
        </header>
    )
}

export default Header