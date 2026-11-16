import { useState } from 'react'
import type { FormEvent } from 'react'
import { BellRing, Link } from 'lucide-react'
import type { NewItem } from '../types'
import { describeError } from '../api'
import './AddItemForm.css'

type Props = {
    onAdd: (item: NewItem) => Promise<void>
}

const emptyForm = {
    name: '',
    productUrl: '',
    targetPrice: '',
}

type FormField = keyof typeof emptyForm

// storeDomain and variantId come from the pasted URL, so they're named after it.
const FIELD_LABELS: Record<string, string> = {
    name: 'Product name',
    productUrl: 'Product URL',
    storeDomain: "The URL's store address",
    variantId: "The URL's variant",
    targetPrice: 'Target price',
}

function parseProductUrl(url: string) {
    const parsed = new URL(url)
    const variant = parsed.searchParams.get('variant')
    return {
        storeDomain: parsed.hostname,
        // Shopify variant ids are numeric; anything else gets the "pick a size" hint below.
        variantId: variant && /^\d+$/.test(variant) ? `gid://shopify/ProductVariant/${variant}` : null,
    }
}

function AddItemForm({ onAdd }: Props) {
    const [form, setForm] = useState(emptyForm)
    const [error, setError] = useState<string | null>(null)
    const [submitting, setSubmitting] = useState(false)

    function update(field: FormField, value: string) {
        setForm((prev) => ({ ...prev, [field]: value }))
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError(null)

        const { storeDomain, variantId } = parseProductUrl(form.productUrl)
        if (!variantId) {
            setError('Pick a size or option on the product page first, then copy the URL. It should include "?variant=".')
            return
        }

        setSubmitting(true)
        try {
            await onAdd({
                name: form.name,
                productUrl: form.productUrl,
                storeDomain,
                variantId,
                targetPrice: Number(form.targetPrice),
            })
            setForm(emptyForm)
        } catch (err) {
            setError(describeError(err, 'Could not add this item. Check the fields and try again.', FIELD_LABELS))
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <form className="add-form" onSubmit={handleSubmit}>
            <div className="add-form__heading">
                <h2>Watch New Product</h2>
            </div>
            <p className="add-form__intro">
                Paste a product link and set a target price.
            </p>

            <label className="field">
                <span>Product name</span>
                <input
                    required
                    value={form.name}
                    onChange={(e) => update('name', e.target.value)}
                    placeholder="e.g. Antique Ivory (Large)"
                />
            </label>

            <label className="field">
                <span>Product URL</span>
                <div className="field__input-wrap">
                    <Link size={16} className="field__icon" />
                    <input
                        required
                        type="url"
                        value={form.productUrl}
                        onChange={(e) => update('productUrl', e.target.value)}
                        placeholder="https://lolablankets.com/products/...?variant=..."
                    />
                </div>
            </label>

            <label className="field">
                <span>Target price (USD)</span>
                <input
                    required
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.targetPrice}
                    onChange={(e) => update('targetPrice', e.target.value)}
                    placeholder="250.00"
                />
                <small>You get an email when the price is at or below this.</small>
            </label>

            {error && <p className="add-form__error">{error}</p>}

            <button className="add-form__submit" type="submit" disabled={submitting}>
                <BellRing size={16} />
                {submitting ? 'Adding...' : 'Start Watching'}
            </button>
        </form>
    )
}

export default AddItemForm