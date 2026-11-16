import { useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { Check, Pencil, X } from 'lucide-react'
import { describeError } from '../api'
import { money } from '../format'
import './TargetPriceEditor.css'

type Props = {
    value: number
    onSave: (newTarget: number) => Promise<void>
}

function TargetPriceEditor({ value, onSave }: Props) {
    const [editing, setEditing] = useState(false)
    const [draft, setDraft] = useState('')
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const parsed = Number(draft)
    const valid = draft.trim() !== '' && Number.isFinite(parsed) && parsed > 0
    const unchanged = valid && parsed === value

    function startEditing() {
        setDraft(String(value))
        setError(null)
        setEditing(true)
    }

    function cancel() {
        setEditing(false)
        setError(null)
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault()
        if (!valid) return
        if (unchanged) {
            cancel()
            return
        }
        setSaving(true)
        setError(null)
        try {
            await onSave(parsed)
            setEditing(false)
        } catch (err) {
            setError(describeError(err, 'Could not save. Try again.', { targetPrice: 'Target' }))
        } finally {
            setSaving(false)
        }
    }

    function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
        if (event.key === 'Escape') {
            // Stop the surrounding <dialog> from treating Esc as "close modal".
            event.preventDefault()
            event.stopPropagation()
            cancel()
        }
    }

    if (!editing) {
        return (
            <div className="target-editor">
                <span className="target-editor__value">{money.format(value)}</span>
                <button className="target-editor__icon" onClick={startEditing} aria-label="Edit target price" title="Edit target">
                    <Pencil size={14} />
                </button>
            </div>
        )
    }

    return (
        <form className="target-editor target-editor--editing" onSubmit={handleSubmit}>
            <div className="target-editor__row">
                <span className="target-editor__currency">$</span>
                <input
                    className="target-editor__input"
                    type="number"
                    inputMode="decimal"
                    min="0.01"
                    step="0.01"
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    onKeyDown={handleKeyDown}
                    aria-label="New target price in USD"
                    aria-invalid={!valid}
                    disabled={saving}
                    autoFocus
                />
            </div>
            <div className="target-editor__actions">
                <button
                    type="submit"
                    className="target-editor__action target-editor__action--save"
                    disabled={!valid || saving}
                    aria-label="Save target price"
                >
                    <Check size={14} />
                    {saving ? 'Saving...' : 'Save'}
                </button>
                <button
                    type="button"
                    className="target-editor__action"
                    onClick={cancel}
                    disabled={saving}
                    aria-label="Cancel editing"
                >
                    <X size={14} />
                    Cancel
                </button>
            </div>
            {error && (
                <span className="target-editor__error" role="alert">
                    {error}
                </span>
            )}
        </form>
    )
}

export default TargetPriceEditor
