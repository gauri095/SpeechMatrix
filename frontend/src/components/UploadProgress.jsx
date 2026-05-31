import { useEffect, useState } from 'react'
import { colors } from '../../utils/styles'

/**
 * UploadProgress — shows two distinct phases:
 *
 *   Phase 1: "Uploading…"  0 → 100%  (Axios onUploadProgress)
 *   Phase 2: "Processing audio…"  indeterminate pulsing bar
 *             (waiting for STT provider response)
 *
 * This split prevents the user seeing a stuck bar at 100% while
 * Deepgram works. The switch to Phase 2 happens automatically
 * when progress reaches 100 — the parent sets isProcessing=true.
 */
export default function UploadProgress({ progress, isProcessing, filename }) {
  const [displayProgress, setDisplayProgress] = useState(0)

  // Smooth the progress bar movement
  useEffect(() => {
    if (progress > displayProgress) {
      const timer = setTimeout(() => setDisplayProgress(progress), 30)
      return () => clearTimeout(timer)
    }
  }, [progress, displayProgress])

  return (
    <div style={{
      padding: '20px',
      background: colors.gray[50],
      border: `1px solid ${colors.gray[200]}`,
      borderRadius: 10,
    }}>
      {/* File name */}
      <p style={{
        fontSize: 13,
        fontWeight: 500,
        color: colors.gray[700],
        margin: '0 0 10px',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
      }}>
        {filename}
      </p>

      {/* Phase label */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 8,
      }}>
        <span style={{ fontSize: 13, color: colors.gray[600] }}>
          {isProcessing ? '🔄 Processing audio…' : `📤 Uploading…`}
        </span>
        {!isProcessing && (
          <span style={{ fontSize: 13, fontWeight: 600, color: colors.primary }}>
            {displayProgress}%
          </span>
        )}
      </div>

      {/* Progress bar */}
      <div style={{
        height: 6,
        borderRadius: 3,
        background: colors.gray[200],
        overflow: 'hidden',
        position: 'relative',
      }}>
        {isProcessing ? (
          /* Indeterminate shimmer */
          <div style={{
            position: 'absolute', top: 0, left: 0,
            height: '100%', width: '40%',
            background: `linear-gradient(90deg, transparent, ${colors.primary}, transparent)`,
            borderRadius: 3,
            animation: 'shimmer 1.4s ease-in-out infinite',
          }} />
        ) : (
          /* Determinate fill */
          <div style={{
            height: '100%',
            width: `${displayProgress}%`,
            background: colors.primary,
            borderRadius: 3,
            transition: 'width 0.3s ease',
          }} />
        )}
      </div>

      {isProcessing && (
        <p style={{ fontSize: 12, color: colors.gray[400], margin: '8px 0 0' }}>
          This can take a few seconds depending on audio length…
        </p>
      )}

      <style>{`
        @keyframes shimmer {
          0%   { left: -40%; }
          100% { left: 100%; }
        }
      `}</style>
    </div>
  )
}
