import { useState, useCallback, useRef } from 'react'

/**
 * useApi — wraps any async function with loading / error / data state.
 *
 * Usage:
 *   const { data, loading, error, execute } = useApi(speechService.getHistory)
 *
 *   useEffect(() => { execute({ page: 0, size: 10 }) }, [])
 *
 *   if (loading) return <Spinner />
 *   if (error)   return <ErrorMessage message={error} />
 *   return <HistoryList items={data.content} />
 */
export function useApi(fn) {
  const [data,    setData]    = useState(null)
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  // Abort controller so stale requests don't overwrite newer results
  const abortRef = useRef(null)

  const execute = useCallback(async (...args) => {
    // Cancel any in-flight request
    if (abortRef.current) abortRef.current.abort()
    abortRef.current = new AbortController()

    setLoading(true)
    setError(null)

    try {
      const result = await fn(...args)
      setData(result)
      return result
    } catch (err) {
      const message = err.apiMessage
        ?? err.message
        ?? 'An unexpected error occurred'
      setError(message)
      throw err
    } finally {
      setLoading(false)
    }
  }, [fn])

  const reset = useCallback(() => {
    setData(null)
    setError(null)
    setLoading(false)
  }, [])

  return { data, loading, error, execute, reset }
}