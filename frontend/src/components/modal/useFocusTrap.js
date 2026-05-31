import { useEffect, useRef } from 'react'

const FOCUSABLE = [
  'a[href]', 'button:not([disabled])', 'textarea:not([disabled])',
  'input:not([disabled])', 'select:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ')

/**
 * useFocusTrap — keeps Tab/Shift+Tab focus inside a container element.
 *
 * Usage:
 *   const trapRef = useFocusTrap(isOpen)
 *   <div ref={trapRef} role="dialog">…</div>
 *
 * When `active` becomes true:
 *  - Saves the previously focused element
 *  - Moves focus to the first focusable element inside the container
 *  - Tab/Shift+Tab cycle stays inside
 *  - When `active` becomes false, restores focus to the saved element
 */
export function useFocusTrap(active = true) {
  const ref          = useRef(null)
  const previousRef  = useRef(null)

  useEffect(() => {
    if (!active) {
      previousRef.current?.focus()
      return
    }

    // Save the current focused element
    previousRef.current = document.activeElement

    // Focus the first focusable element inside the container
    const el = ref.current
    if (!el) return
    const focusables = Array.from(el.querySelectorAll(FOCUSABLE))
    focusables[0]?.focus()

    const handleKeyDown = (e) => {
      if (e.key !== 'Tab') return
      const focusable = Array.from(el.querySelectorAll(FOCUSABLE))
      if (focusable.length === 0) { e.preventDefault(); return }

      const first = focusable[0]
      const last  = focusable[focusable.length - 1]

      if (e.shiftKey) {
        // Shift+Tab: wrap from first to last
        if (document.activeElement === first) {
          e.preventDefault()
          last.focus()
        }
      } else {
        // Tab: wrap from last to first
        if (document.activeElement === last) {
          e.preventDefault()
          first.focus()
        }
      }
    }

    el.addEventListener('keydown', handleKeyDown)
    return () => {
      el.removeEventListener('keydown', handleKeyDown)
      previousRef.current?.focus()
    }
  }, [active])

  return ref
}