import { useEffect, useCallback } from 'react'

/**
 * useKeyboard — registers a keyboard shortcut handler.
 *
 * Usage:
 *   // Ctrl+K or Cmd+K to open search
 *   useKeyboard('k', handler, { ctrl: true })
 *
 *   // Escape to close modal
 *   useKeyboard('Escape', onClose)
 *
 *   // ? to open help
 *   useKeyboard('?', openHelp)
 *
 * Options:
 *   ctrl    bool — require Ctrl (or Cmd on Mac)
 *   shift   bool — require Shift
 *   alt     bool — require Alt
 *   enabled bool — set false to unregister (default true)
 */
export function useKeyboard(key, handler, options = {}) {
  const { ctrl = false, shift = false, alt = false, enabled = true } = options

  const memoHandler = useCallback(handler, [handler])

  useEffect(() => {
    if (!enabled) return

    const onKeyDown = (e) => {
      const isMeta = e.ctrlKey || e.metaKey   // treat Cmd same as Ctrl
      if (ctrl  && !isMeta)   return
      if (shift && !e.shiftKey) return
      if (alt   && !e.altKey)   return
      if (e.key !== key)        return

      // Don't fire when typing in an input
      const tag = (e.target?.tagName ?? '').toLowerCase()
      if (['input','textarea','select'].includes(tag) && key !== 'Escape') return

      memoHandler(e)
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [key, ctrl, shift, alt, enabled, memoHandler])
}