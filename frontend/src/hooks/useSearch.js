import { useState, useCallback, useEffect, useRef } from 'react'
import speechService from '../services/speechService'

/**
 * useSearch — debounced full-text transcript search.
 *
 * Waits 350ms after the user stops typing before firing the API call.
 * Cancels in-flight requests when a newer query arrives.
 *
 * Returns:
 *   query          current search string
 *   setQuery       update the search string
 *   results        SearchResult[] with highlightedPreview
 *   loading        bool
 *   error          string | null
 *   clear()        reset query + results
 *   hasResults     bool — true when results are non-empty
 *   isActive       bool — true when query.length >= 2
 */
export function useSearch() {
  const [query,   setQuery]   = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  const debounceRef = useRef(null)
  const abortRef    = useRef(null)

  useEffect(() => {
    // Cancel previous debounce timer
    clearTimeout(debounceRef.current)

    if (!query || query.trim().length < 2) {
      setResults([])
      setLoading(false)
      setError(null)
      return
    }

    setLoading(true)
    setError(null)

    debounceRef.current = setTimeout(async () => {
      // Cancel any in-flight search
      abortRef.current?.abort?.()
      abortRef.current = new AbortController()

      try {
        const response = await speechService.search(
          query.trim(),
          { paged: true, page: 0, size: 20 }
        )
        setResults(response.content ?? [])
        setError(null)
      } catch (err) {
        if (err.name !== 'CanceledError' && err.name !== 'AbortError') {
          setError(err.apiMessage ?? 'Search failed')
          setResults([])
        }
      } finally {
        setLoading(false)
      }
    }, 350)

    return () => clearTimeout(debounceRef.current)
  }, [query])

  const clear = useCallback(() => {
    setQuery('')
    setResults([])
    setError(null)
  }, [])

  return {
    query,
    setQuery,
    results,
    loading,
    error,
    clear,
    hasResults: results.length > 0,
    isActive:   query.trim().length >= 2,
  }
}