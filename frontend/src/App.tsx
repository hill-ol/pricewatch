import { useEffect, useState } from 'react'
import type { NewItem, WatchedItem } from './types'
import Header from './components/Header'
import AddItemForm from './components/AddItemForm'
import ItemCard from './components/ItemCard'
import ItemDetails from './components/ItemDetails'
import { isAtTarget } from './itemStatus'
import './App.css'
import { CircleAlert, LoaderCircle, PackageOpen } from 'lucide-react'

type LoadStatus = 'loading' | 'error' | 'ready'

async function fetchItems(): Promise<WatchedItem[]> {
  const res = await fetch('/items')
  if (!res.ok) {
    throw new Error(`GET /items failed with ${res.status}`)
  }
  return res.json()
}

function App() {
  const [items, setItems] = useState<WatchedItem[]>([])
  const [status, setStatus] = useState<LoadStatus>('loading')
  const [notice, setNotice] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)

  function loadItems() {
    return fetchItems()
        .then((loaded) => {
          setItems(loaded)
          setStatus('ready')
        })
        .catch(() => setStatus('error'))
  }

  useEffect(() => {
    loadItems()
  }, [])

  function handleRetry() {
    setStatus('loading')
    loadItems()
  }

  async function handleAdd(newItem: NewItem) {
    const res = await fetch('/items', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newItem),
    })
    if (!res.ok) {
      throw new Error('Failed to add item')
    }
    const created: WatchedItem = await res.json()
    setItems((prev) => [...prev, created])
  }

  async function handleUpdateTarget(id: number, targetPrice: number) {
    const res = await fetch(`/items/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ targetPrice }),
    })
    if (!res.ok) {
      throw new Error(`PATCH failed with ${res.status}`)
    }
    const updated: WatchedItem = await res.json()
    setItems((prev) => prev.map((item) => (item.id === id ? updated : item)))
  }

  async function handleDelete(id: number) {
    setNotice(null)
    try {
      const res = await fetch(`/items/${id}`, { method: 'DELETE' })
      if (!res.ok) {
        throw new Error(`DELETE failed with ${res.status}`)
      }
      setItems((prev) => prev.filter((item) => item.id !== id))
    } catch {
      setNotice('Could not remove that item. Is the backend running?')
    }
  }

  const triggeredCount = items.filter(isAtTarget).length
  const selectedItem = items.find((item) => item.id === selectedId)

  function renderItems() {
    if (status === 'loading') {
      return (
          <div className="empty-state">
            <LoaderCircle size={32} className="spin" />
            <p>Loading your watched items...</p>
          </div>
      )
    }
    if (status === 'error') {
      return (
          <div className="empty-state empty-state--error">
            <CircleAlert size={32} />
            <p className="empty-state__title">Couldn't reach the server</p>
            <p>Make sure the backend is running on port 8080, then try again.</p>
            <button className="empty-state__retry" onClick={handleRetry}>
              Retry
            </button>
          </div>
      )
    }
    if (items.length === 0) {
      return (
          <div className="empty-state">
            <PackageOpen size={32} />
            <p className="empty-state__title">Nothing watched yet</p>
            <p>Add a product with the form to start tracking its price.</p>
          </div>
      )
    }
    return items.map((item) => (
        <ItemCard key={item.id} item={item} onOpen={setSelectedId} onDelete={handleDelete} />
    ))
  }

  return (
      <>
        <Header monitoredCount={items.length} triggeredCount={triggeredCount} />
        <div className="layout">
          <AddItemForm onAdd={handleAdd} />
          <section className="items">
            {notice && (
                <p className="notice" role="alert">
                  {notice}
                </p>
            )}
            {renderItems()}
          </section>
        </div>
        {selectedItem && (
            <ItemDetails
                key={selectedItem.id}
                item={selectedItem}
                onClose={() => setSelectedId(null)}
                onUpdateTarget={handleUpdateTarget}
            />
        )}
      </>
  )
}

export default App
