import { colors } from '../../utils/styles'

/**
 * RecordingControls — strip of secondary buttons shown while recording.
 *
 * Shown during 'recording' phase:
 *   [Pause]   [Discard]
 *
 * Shown during 'paused' phase:
 *   [Resume]  [Discard]
 */
export default function RecordingControls({ phase, onPause, onResume, onDiscard }) {
  if (phase !== 'recording' && phase !== 'paused') return null

  const isRecording = phase === 'recording'

  return (
    <div style={{
      display: 'flex', gap: 10, justifyContent: 'center',
      marginTop: 8,
    }}>

      {/* Pause / Resume */}
      <button
        onClick={isRecording ? onPause : onResume}
        style={secondaryBtn}
        aria-label={isRecording ? 'Pause recording' : 'Resume recording'}
      >
        {isRecording ? '⏸ Pause' : '▶ Resume'}
      </button>

      {/* Discard */}
      <button
        onClick={onDiscard}
        style={{ ...secondaryBtn, color: '#DC2626', borderColor: '#FECACA' }}
        aria-label="Discard recording"
      >
        🗑 Discard
      </button>
    </div>
  )
}

const secondaryBtn = {
  padding: '6px 14px',
  fontSize: 13,
  fontWeight: 500,
  background: '#fff',
  border: `1px solid ${colors.gray[200]}`,
  borderRadius: 8,
  cursor: 'pointer',
  color: colors.gray[600],
  fontFamily: 'inherit',
}
