import { useEffect, useState } from 'react'
import type { NewItem, WatchedItem } from './types'
import { createItem, deleteItem, getItems, updateTargetPrice } from './api'
import Header from './components/Header'
import AddItemForm from './components/AddItemForm'
import ItemCard from './items/ItemCard'
import ItemDetails from './items/ItemDetails'
import { isAtTarget } from './items/itemStatus'
import './App.css'
import { CircleAlert, LoaderCircle, PackageOpen } from 'lucide-react'

type LoadStatus = 'loading' | 'error' | 'ready'

function App() {
  const [items, setItems] = useState<WatchedItem[]>([])
  const [status, setStatus] = useState<LoadStatus>('loading')
  const [notice, setNotice] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)

  function loadItems() {
    return getItems()
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
    const created = await createItem(newItem)
    setItems((prev) => [...prev, created])
  }

  async function handleUpdateTarget(id: number, targetPrice: number) {
    const updated = await updateTargetPrice(id, targetPrice)
    setItems((prev) => prev.map((item) => (item.id === id ? updated : item)))
  }

  async function handleDelete(id: number) {
    setNotice(null)
    try {
      await deleteItem(id)
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
