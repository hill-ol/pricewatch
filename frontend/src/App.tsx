import { useEffect, useState } from 'react'
import type { WatchedItem } from './types'
import ItemCard from './components/ItemCard'
import Header from './components/Header'

function App() {
  const [items, setItems] = useState<WatchedItem[]>([])

  useEffect(() => {
    fetch('/items')
        .then((res) => res.json())
        .then(setItems)
  }, [])

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
        <main style={{ maxWidth: 720, margin: '0 auto', display: 'grid', gap: 16 }}>
          {items.map((item) => (
              <ItemCard key={item.id} item={item} onDelete={handleDelete} />
          ))}
        </main>
      </>
  )
}

export default App