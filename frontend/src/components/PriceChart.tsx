import { useState } from 'react'
import type { PointerEvent } from 'react'
import type { PricePoint } from '../types'
import { dateTime, money, shortDate, shortTime } from '../format'
import './PriceChart.css'

type Props = {
    points: PricePoint[]
    targetPrice: number
}

const WIDTH = 600
const HEIGHT = 220
const MARGIN = { top: 16, right: 16, bottom: 28, left: 56 }
const DAY_MS = 24 * 60 * 60 * 1000

// Round tick values (e.g. $320, $340, $360) that fall inside [min, max].
function niceTicks(min: number, max: number, target = 3): number[] {
    const rough = (max - min) / target
    const magnitude = 10 ** Math.floor(Math.log10(rough))
    const step = [1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((s) => s >= rough) ?? rough
    const ticks: number[] = []
    for (let tick = Math.ceil(min / step) * step; tick <= max; tick += step) {
        ticks.push(Number(tick.toFixed(2)))
    }
    return ticks
}

function PriceChart({ points, targetPrice }: Props) {
    const [hovered, setHovered] = useState<number | null>(null)

    const times = points.map((p) => new Date(p.checkedAt).getTime())
    const prices = points.map((p) => p.price)

    const low = Math.min(...prices, targetPrice)
    const high = Math.max(...prices, targetPrice)
    const pad = (high - low) * 0.15 || high * 0.05 || 1
    const yMin = low - pad
    const yMax = high + pad

    const t0 = times[0]
    const tEnd = times[times.length - 1]
    const timeSpan = tEnd - t0 || 1

    const plotLeft = MARGIN.left
    const plotRight = WIDTH - MARGIN.right
    const plotTop = MARGIN.top
    const plotBottom = HEIGHT - MARGIN.bottom

    const x = (t: number) => plotLeft + ((t - t0) / timeSpan) * (plotRight - plotLeft)
    const y = (price: number) => plotBottom - ((price - yMin) / (yMax - yMin)) * (plotBottom - plotTop)

    const coords = points.map((p, i) => [x(times[i]), y(p.price)] as const)
    const linePath = coords.map(([cx, cy], i) => `${i === 0 ? 'M' : 'L'}${cx},${cy}`).join(' ')
    const areaPath = `${linePath} L${coords[coords.length - 1][0]},${plotBottom} L${coords[0][0]},${plotBottom} Z`

    const yTicks = niceTicks(yMin, yMax)
    const xTicks = [t0, t0 + timeSpan / 2, tEnd]
    const formatTick = timeSpan < 1.5 * DAY_MS ? shortTime : shortDate
    const targetY = y(targetPrice)

    function handlePointerMove(event: PointerEvent<SVGSVGElement>) {
        const rect = event.currentTarget.getBoundingClientRect()
        const viewX = ((event.clientX - rect.left) / rect.width) * WIDTH
        let nearest = 0
        coords.forEach(([cx], i) => {
            if (Math.abs(cx - viewX) < Math.abs(coords[nearest][0] - viewX)) nearest = i
        })
        setHovered(nearest)
    }

    const active = hovered === null ? null : { point: points[hovered], cx: coords[hovered][0], cy: coords[hovered][1] }
    const tooltipAlign = active === null ? '' : active.cx > WIDTH * 0.75 ? 'right' : active.cx < WIDTH * 0.25 ? 'left' : ''

    return (
        <div className="price-chart">
            <svg
                viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
                className="price-chart__svg"
                onPointerMove={handlePointerMove}
                onPointerLeave={() => setHovered(null)}
                role="img"
                aria-label={`Price history chart with ${points.length} checks`}
            >
                <defs>
                    <linearGradient id="price-chart-fill" x1="0" x2="0" y1="0" y2="1">
                        <stop offset="0%" stopColor="var(--color-accent)" stopOpacity="0.18" />
                        <stop offset="100%" stopColor="var(--color-accent)" stopOpacity="0" />
                    </linearGradient>
                </defs>

                {yTicks.map((tick) => (
                    <g key={tick}>
                        <line className="price-chart__grid" x1={plotLeft} x2={plotRight} y1={y(tick)} y2={y(tick)} />
                        <text className="price-chart__axis" x={plotLeft - 10} y={y(tick)} textAnchor="end" dominantBaseline="middle">
                            {money.format(tick)}
                        </text>
                    </g>
                ))}

                {xTicks.map((tick, i) => (
                    <text
                        key={tick}
                        className="price-chart__axis"
                        x={x(tick)}
                        y={HEIGHT - 8}
                        textAnchor={i === 0 ? 'start' : i === xTicks.length - 1 ? 'end' : 'middle'}
                    >
                        {formatTick.format(tick)}
                    </text>
                ))}

                <line className="price-chart__target" x1={plotLeft} x2={plotRight} y1={targetY} y2={targetY} />
                <text className="price-chart__target-label" x={plotRight} y={targetY - 6} textAnchor="end">
                    Target {money.format(targetPrice)}
                </text>

                <path className="price-chart__area" d={areaPath} />
                <path className="price-chart__line" d={linePath} />

                {active && (
                    <g>
                        <line className="price-chart__guide" x1={active.cx} x2={active.cx} y1={plotTop} y2={plotBottom} />
                        <circle className="price-chart__dot" cx={active.cx} cy={active.cy} r={5} />
                    </g>
                )}
            </svg>

            {active && (
                <div
                    className={`price-chart__tooltip price-chart__tooltip--${tooltipAlign || 'center'}`}
                    style={{ left: `${(active.cx / WIDTH) * 100}%`, top: `${(active.cy / HEIGHT) * 100}%` }}
                >
                    <strong>{money.format(active.point.price)}</strong>
                    <span>{dateTime.format(new Date(active.point.checkedAt))}</span>
                </div>
            )}
        </div>
    )
}

export default PriceChart
