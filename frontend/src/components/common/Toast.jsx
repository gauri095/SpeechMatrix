import { useState, useEffect, useCallback, createContext, useContext } from 'react'
import { colors } from '../../utils/styles'

// ── Context ────────────────────────────────────────────────────────────────────
const ToastContext = createContext(null)

/**
 * ToastProvider — wraps the app to enable toast notifications.
 *
 * Usage:
 *   const toast = useToast()
 *   toast.success('Transcript saved!')
 *   toast.error('Upload failed')
 *   toast.info('Processing…')
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const add = useCallback((message, type = 'info', duration = 3500) => {
    const id = Date.now()
    setToasts(t => [...t, { id, message, type }])
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), duration)
  }, [])

  const toast = {
    success: (msg, dur) => add(msg, 'success', dur),
    error:   (msg, dur) => add(msg, 'error',   dur ?? 5000),
    info:    (msg, dur) => add(msg, 'info',     dur),
  }

  return (
    <ToastContext.Provider value={toast}>
      {children}
      {/* Stack in top-right corner */}
      <div
        aria-live="polite"
        aria-atomic="false"
        style={{
          position: 'fixed', top: 20, right: 20,
          display: 'flex', flexDirection: 'column', gap: 8,
          zIndex: 9999, maxWidth: 360,
        }}
      >
        {toasts.map(t => (
          <ToastItem key={t.id} {...t} onClose={() =>
            setToasts(x => x.filter(i => i.id !== t.id))} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function ToastItem({ message, type, onClose }) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    // Tiny delay so CSS transition fires
    const t = setTimeout(() => setVisible(true), 10)
    return () => clearTimeout(t)
  }, [])

  const bg = {
    success: colors.primary,
    error:   colors.danger,
    info:    colors.gray[700],
  }[type] ?? colors.gray[700]

  return (
    <div
      role="status"
      style={{
        background: bg,
        color: '#fff',
        padding: '12px 16px',
        borderRadius: 8,
        fontSize: 14,
        fontWeight: 500,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        boxShadow: '0 4px 12px rgb(0 0 0 / 0.15)',
        opacity:   visible ? 1 : 0,
        transform: visible ? 'translateX(0)' : 'translateX(20px)',
        transition: 'opacity 0.2s, transform 0.2s',
        cursor: 'default',
        minWidth: 200,
      }}
    >
      <span>{message}</span>
      <button
        onClick={onClose}
        aria-label="Dismiss notification"
        style={{
          background: 'none', border: 'none', color: 'rgba(255,255,255,0.8)',
          cursor: 'pointer', fontSize: 16, lineHeight: 1, padding: 0,
          flexShrink: 0,
        }}
      >
        ×
      </button>
    </div>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be inside <ToastProvider>')
  return ctx
}
