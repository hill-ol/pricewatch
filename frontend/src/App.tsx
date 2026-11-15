import { useEffect, useState } from 'react'
import type { NewItem, WatchedItem } from './types'
import Header from './components/Header'
import AddItemForm from './components/AddItemForm'
import ItemCard from './components/ItemCard'
import './App.css'
import {PackageOpen} from "lucide-react";

function App() {
  const [items, setItems] = useState<WatchedItem[]>([])

  useEffect(() => {
    fetch('/items')
        .then((res) => res.json())
        .then(setItems)
  }, [])

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

  function handleDelete(id: number) {
    fetch(`/items/${id}`, { method: 'DELETE' }).then((res) => {
      if (res.ok) {
        setItems((prev) => prev.filter((item) => item.id !== id))
      }
    })
  }

  const triggeredCount = items.filter((item) => item.alerted).length

  return (
      <>
        <Header monitoredCount={items.length} triggeredCount={triggeredCount} />
        <div className="layout">
          <AddItemForm onAdd={handleAdd} />
          <section className="items">
            <section className="items">
              {items.length === 0 ? (
                  <div className="empty-state">
                    <PackageOpen size={32} />
                    <p className="empty-state__title">Nothing watched yet</p>
                    <p>Add a product with the form to start tracking its price.</p>
                  </div>
              ) : (
                  items.map((item) => (
                      <ItemCard key={item.id} item={item} onDelete={handleDelete} />
                  ))
              )}
            </section>
          </section>
        </div>
      </>
  )
}

export default App