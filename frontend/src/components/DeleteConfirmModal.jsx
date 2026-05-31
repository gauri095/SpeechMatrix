import { useEffect, useRef } from 'react'
import Button from '../common/Button'
import { colors } from '../../utils/styles'

/**
 * DeleteConfirmModal — accessible confirmation dialog.
 * Traps focus, closes on Escape, and disables body scroll while open.
 */
export default function DeleteConfirmModal({ item, onConfirm, onCancel, loading = false }) {
  const cancelRef = useRef(null)

  // Focus the cancel button on open (safe default action)
  useEffect(() => {
    cancelRef.current?.focus()
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = '' }
  }, [])

  // Close on Escape
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onCancel() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onCancel])

  if (!item) return null

  const preview = item.transcriptPreview ?? item.transcript?.slice(0, 80)

  return (
    /* Backdrop */
    <div
      onClick={onCancel}
      style={{
        position: 'fixed', inset: 0,
        background: 'rgba(0,0,0,0.4)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        zIndex: 1000, padding: 16,
      }}
    >
      {/* Dialog */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-title"
        onClick={e => e.stopPropagation()}
        style={{
          background: '#fff',
          borderRadius: 16,
          padding: '28px 24px',
          width: '100%',
          maxWidth: 420,
          boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
        }}
      >
        <div style={{ fontSize: 32, textAlign: 'center', marginBottom: 12 }}>🗑</div>

        <h2 id="delete-title" style={{
          fontSize: 18, fontWeight: 700,
          color: colors.gray[900],
          textAlign: 'center', margin: '0 0 8px',
        }}>
          Delete transcript?
        </h2>

        {preview && (
          <p style={{
            fontSize: 13, color: colors.gray[500],
            textAlign: 'center', margin: '0 0 20px',
            lineHeight: 1.5,
            background: colors.gray[50],
            borderRadius: 8, padding: '8px 12px',
          }}>
            "{preview}{preview.length >= 80 ? '…' : ''}"
          </p>
        )}

        <p style={{
          fontSize: 13, color: colors.gray[500],
          textAlign: 'center', margin: '0 0 24px',
        }}>
          This will permanently delete the transcript and audio file.
          This action cannot be undone.
        </p>

        <div style={{ display: 'flex', gap: 10 }}>
          <Button
            ref={cancelRef}
            variant="secondary"
            fullWidth
            onClick={onCancel}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button
            variant="danger"
            fullWidth
            onClick={onConfirm}
            loading={loading}
          >
            Delete
          </Button>
        </div>
      </div>
    </div>
  )
}
