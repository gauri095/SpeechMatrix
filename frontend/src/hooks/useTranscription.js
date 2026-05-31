import { useState, useCallback, useEffect } from 'react'
import speechService from '../services/speechService'

/**
 * useTranscriptions — manages paginated transcript history state.
 *
 * Provides load(), loadMore(), refresh(), and full filter support.
 * Designed for the HistoryPage — handles pagination internally.
 */
export function useTranscriptions(initialFilters = {}) {
  const [items,      setItems]      = useState([])
  const [loading,    setLoading]    = useState(false)
  const [error,      setError]      = useState(null)
  const [page,       setPage]       = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [total,      setTotal]      = useState(0)
  const [filters,    setFilters]    = useState({
    size:    10,
    sortBy:  'createdAt',
    sortDir: 'desc',
    ...initialFilters,
  })

  const load = useCallback(async (pageNum = 0, newFilters = null) => {
    setLoading(true)
    setError(null)
    try {
      const params  = { ...filters, ...(newFilters ?? {}), page: pageNum }
      const result  = await speechService.getHistory(params)
      setItems(pageNum === 0 ? result.content : prev => [...prev, ...result.content])
      setPage(result.page)
      setTotalPages(result.totalPages)
      setTotal(result.totalElements)
    } catch (err) {
      setError(err.apiMessage ?? 'Failed to load transcriptions')
    } finally {
      setLoading(false)
    }
  }, [filters])

  const loadMore = useCallback(() => {
    if (page + 1 < totalPages) load(page + 1)
  }, [load, page, totalPages])

  const refresh = useCallback(() => load(0), [load])

  const applyFilters = useCallback((newFilters) => {
    setFilters(f => ({ ...f, ...newFilters }))
    setItems([])
    setPage(0)
  }, [])

  const removeItem = useCallback((id) => {
    setItems(prev => prev.filter(t => t.id !== id))
    setTotal(n => n - 1)
  }, [])

  const hasMore = page + 1 < totalPages

  return {
    items, loading, error, page, totalPages, total, hasMore,
    filters, load, loadMore, refresh, applyFilters, removeItem,
  }
}