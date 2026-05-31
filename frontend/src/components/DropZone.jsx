import { useState, useRef, useCallback } from 'react'
import { colors } from '../../utils/styles'
import { formatFileSize } from '../../utils/formatters'

const ACCEPTED = ['audio/mpeg', 'audio/wav', 'audio/x-wav', 'audio/mp4',
                  'audio/flac', 'audio/ogg', 'audio/webm', 'audio/x-m4a',
                  'video/mp4']  // some browsers report m4a as video/mp4

const ACCEPTED_EXTENSIONS = '.mp3,.wav,.m4a,.flac,.ogg,.webm,.mp4'
const MAX_SIZE_BYTES       = 50 * 1024 * 1024   // 50 MB

/**
 * DropZone — handles both click-to-browse and drag-and-drop file selection.
 *
 * Validates format and size client-side before the file ever leaves the browser,
 * giving instant feedback instead of waiting for a 422 from the server.
 *
 * Props:
 *   onFile(File)   called with the selected File object when valid
 *   disabled       bool — greys out zone during upload
 */
export default function DropZone({ onFile, disabled = false }) {
  const [dragOver,  setDragOver]  = useState(false)
  const [localError, setLocalError] = useState(null)
  const inputRef = useRef(null)

  // ── Validation ─────────────────────────────────────────────────────────────
  const validate = useCallback((file) => {
    if (!file) return 'No file selected'

    // Size check
    if (file.size > MAX_SIZE_BYTES) {
      return `File too large: ${formatFileSize(file.size)}. Maximum is 50 MB.`
    }

    // MIME type check (some browsers give application/octet-stream for audio)
    const ext = file.name.split('.').pop()?.toLowerCase()
    const validExt = ['mp3','wav','m4a','flac','ogg','webm','mp4'].includes(ext)
    const validMime = ACCEPTED.includes(file.type) || file.type === ''  // empty = trust extension

    if (!validExt && !validMime) {
      return `Unsupported format: .${ext}. Use MP3, WAV, M4A, FLAC, OGG, or WebM.`
    }

    return null   // valid
  }, [])

  const handleFile = useCallback((file) => {
    const err = validate(file)
    if (err) {
      setLocalError(err)
      return
    }
    setLocalError(null)
    onFile(file)
  }, [validate, onFile])

  // ── Input change ───────────────────────────────────────────────────────────
  const handleInputChange = (e) => {
    const file = e.target.files?.[0]
    if (file) handleFile(file)
    // Reset input so selecting the same file again fires onChange
    e.target.value = ''
  }

  // ── Drag events ────────────────────────────────────────────────────────────
  const handleDragOver = (e) => {
    e.preventDefault()
    if (!disabled) setDragOver(true)
  }
  const handleDragLeave = (e) => {
    // Only clear if leaving the zone entirely (not child elements)
    if (!e.currentTarget.contains(e.relatedTarget)) setDragOver(false)
  }
  const handleDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    if (disabled) return
    const file = e.dataTransfer.files?.[0]
    if (file) handleFile(file)
  }

  // ── Style ──────────────────────────────────────────────────────────────────
  const borderColor = localError ? colors.danger
                    : dragOver   ? colors.primary
                    :              colors.gray[300]

  const bgColor = dragOver   ? colors.primaryLight
                : disabled   ? colors.gray[100]
                :              '#fff'

  return (
    <div>
      <div
        role="button"
        tabIndex={disabled ? -1 : 0}
        aria-label="Drop audio file here or click to browse"
        onClick={() => !disabled && inputRef.current?.click()}
        onKeyDown={(e) => e.key === 'Enter' && !disabled && inputRef.current?.click()}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        style={{
          border: `2px dashed ${borderColor}`,
          borderRadius: 12,
          padding: '40px 24px',
          textAlign: 'center',
          cursor: disabled ? 'not-allowed' : 'pointer',
          background: bgColor,
          transition: 'border-color 0.2s, background 0.2s',
          outline: 'none',
          userSelect: 'none',
        }}
      >
        {/* Upload icon */}
        <div style={{ fontSize: 40, marginBottom: 12, lineHeight: 1 }}>
          {dragOver ? '📂' : '🎵'}
        </div>

        <p style={{ fontWeight: 600, fontSize: 15, color: colors.gray[800], margin: '0 0 4px' }}>
          {dragOver ? 'Drop to upload' : 'Drop audio file here'}
        </p>

        <p style={{ fontSize: 13, color: colors.gray[500], margin: '0 0 16px' }}>
          or <span style={{ color: colors.primary, fontWeight: 500 }}>click to browse</span>
        </p>

        {/* Format pills */}
        <div style={{ display: 'flex', gap: 6, justifyContent: 'center', flexWrap: 'wrap' }}>
          {['MP3', 'WAV', 'M4A', 'FLAC', 'OGG', 'WebM'].map(fmt => (
            <span key={fmt} style={{
              fontSize: 11, fontWeight: 500, fontFamily: 'monospace',
              padding: '2px 8px',
              background: colors.gray[100],
              border: `1px solid ${colors.gray[200]}`,
              borderRadius: 4,
              color: colors.gray[600],
            }}>
              {fmt}
            </span>
          ))}
        </div>

        <p style={{ fontSize: 12, color: colors.gray[400], margin: '10px 0 0' }}>
          Maximum 50 MB
        </p>
      </div>

      {/* Client-side validation error */}
      {localError && (
        <p role="alert" style={{
          fontSize: 13, color: colors.danger,
          marginTop: 8, display: 'flex', gap: 4, alignItems: 'center',
        }}>
          ⚠ {localError}
        </p>
      )}

      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_EXTENSIONS}
        onChange={handleInputChange}
        style={{ display: 'none' }}
        aria-hidden="true"
        tabIndex={-1}
      />
    </div>
  )
}
