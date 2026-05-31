import { colors } from '../../utils/styles'
import { formatFileSize } from '../../utils/formatters'

const MIME_ICON = {
  'audio/mpeg': '🎵', 'audio/mp3': '🎵',
  'audio/wav': '🔊',  'audio/x-wav': '🔊',
  'audio/flac': '🎶', 'audio/ogg': '🎶',
  'audio/mp4': '🎧',  'audio/x-m4a': '🎧',
  'audio/webm': '🎙',
}

/**
 * SelectedFile — card shown after the user picks a file.
 *
 * Displays the filename, size, and format icon.
 * Includes an × button to remove the file and choose another.
 */
export default function SelectedFile({ file, onRemove, disabled = false }) {
  if (!file) return null

  const ext  = file.name.split('.').pop()?.toUpperCase() ?? ''
  const icon = MIME_ICON[file.type] ?? '📁'

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      padding: '12px 16px',
      background: colors.primaryLight,
      border: `1px solid ${colors.primaryMid}`,
      borderRadius: 10,
    }}>
      {/* Icon */}
      <span style={{ fontSize: 28, flexShrink: 0 }}>{icon}</span>

      {/* File info */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <p style={{
          fontWeight: 600,
          fontSize: 14,
          color: colors.gray[800],
          margin: 0,
          whiteSpace: 'nowrap',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
        }}>
          {file.name}
        </p>
        <p style={{ fontSize: 12, color: colors.gray[500], margin: '2px 0 0' }}>
          {formatFileSize(file.size)} · {ext}
        </p>
      </div>

      {/* Remove button */}
      {!disabled && (
        <button
          onClick={onRemove}
          aria-label="Remove file"
          style={{
            flexShrink: 0,
            width: 28, height: 28,
            borderRadius: '50%',
            border: 'none',
            background: 'rgba(0,0,0,0.08)',
            color: colors.gray[600],
            cursor: 'pointer',
            fontSize: 16,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            transition: 'background 0.15s',
          }}
        >
          ×
        </button>
      )}
    </div>
  )
}
