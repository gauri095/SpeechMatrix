import { useState, useEffect } from 'react'

/**
 * useMediaQuery — returns true when the given CSS media query matches.
 *
 * Usage:
 *   const isMobile  = useMediaQuery('(max-width: 480px)')
 *   const isTablet  = useMediaQuery('(max-width: 768px)')
 *   const isDesktop = useMediaQuery('(min-width: 1024px)')
 *
 * Pre-defined breakpoints:
 *   useIsMobile()   → ≤ 480px
 *   useIsTablet()   → ≤ 768px
 *   useIsDesktop()  → ≥ 1024px
 */
export function useMediaQuery(query) {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined') return false
    return window.matchMedia(query).matches
  })

  useEffect(() => {
    if (typeof window === 'undefined') return
    const mql = window.matchMedia(query)
    const handler = (e) => setMatches(e.matches)
    mql.addEventListener('change', handler)
    setMatches(mql.matches)
    return () => mql.removeEventListener('change', handler)
  }, [query])

  return matches
}

export const useIsMobile  = () => useMediaQuery('(max-width: 480px)')
export const useIsTablet  = () => useMediaQuery('(max-width: 768px)')
export const useIsDesktop = () => useMediaQuery('(min-width: 1024px)')