import { colors } from '../../utils/styles'

/**
 * RecordButton — the main mic button.
 *
 * States:
 *   idle      → green mic, "Tap to record"
 *   recording → red stop, pulsing ring, "Recording…"
 *   paused    → amber resume icon, "Paused"
 *   recorded  → green check, "Recording complete"
 */
export default function RecordButton({ phase, duration, onStart, onStop, onPause, onResume, disabled = false }) {
  const isRecording = phase === 'recording'
  const isPaused    = phase === 'paused'
  const isDone      = phase === 'recorded' || phase === 'done'

  const bgColor  = isRecording ? '#DC2626'
                 : isPaused    ? '#D97706'
                 : isDone      ? colors.primary
                 :               colors.primary

  const ringColor = isRecording ? 'rgba(220,38,38,0.2)'
                  : isPaused    ? 'rgba(217,119,6,0.2)'
                  :               colors.primaryLight

  const icon = isRecording ? '⏹'
             : isPaused    ? '▶'
             : isDone      ? '✓'
             :               '🎙'

  const label = isRecording ? 'Stop recording'
              : isPaused    ? 'Resume recording'
              : isDone      ? 'Recording ready'
              :               'Start recording'

  const handleClick = () => {
    if (disabled) return
    if (isRecording) onStop?.()
    else if (isPaused) onResume?.()
    else onStart?.()
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>

      {/* Outer pulse ring */}
      <div style={{ position: 'relative', display: 'inline-flex' }}>
        {isRecording && (
          <>
            <div style={{
              position: 'absolute', inset: -12,
              borderRadius: '50%',
              background: ringColor,
              animation: 'pulse-ring 1.5s ease-out infinite',
            }} />
            <div style={{
              position: 'absolute', inset: -6,
              borderRadius: '50%',
              background: ringColor,
              animation: 'pulse-ring 1.5s ease-out 0.4s infinite',
            }} />
          </>
        )}

        <button
          onClick={handleClick}
          disabled={disabled}
          aria-label={label}
          style={{
            width: 80, height: 80,
            borderRadius: '50%',
            background: disabled ? colors.gray[300] : bgColor,
            border: 'none',
            cursor: disabled ? 'not-allowed' : 'pointer',
            fontSize: 28,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: `0 4px 20px ${isRecording ? 'rgba(220,38,38,0.35)' : 'rgba(29,158,117,0.35)'}`,
            transition: 'background 0.2s, transform 0.1s, box-shadow 0.2s',
            transform: 'scale(1)',
            position: 'relative', zIndex: 1,
          }}
          onMouseDown={e => { e.currentTarget.style.transform = 'scale(0.94)' }}
          onMouseUp={e =>   { e.currentTarget.style.transform = 'scale(1)' }}
          onMouseLeave={e =>{ e.currentTarget.style.transform = 'scale(1)' }}
        >
          {icon}
        </button>
      </div>

      {/* Label */}
      <p style={{
        fontSize: 13,
        fontWeight: 500,
        color: isRecording ? '#DC2626' : colors.gray[600],
        margin: 0,
        letterSpacing: '0.03em',
        textTransform: 'uppercase',
        fontSize: 11,
      }}>
        {isRecording
          ? `Recording — ${formatTimer(duration)}`
          : isPaused ? 'Paused — tap to resume'
          : isDone   ? 'Tap mic to record again'
          : 'Tap to record'}
      </p>

      <style>{`
        @keyframes pulse-ring {
          0%   { transform: scale(1);   opacity: 0.7; }
          100% { transform: scale(1.5); opacity: 0;   }
        }
      `}</style>
    </div>
  )
}

function formatTimer(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
