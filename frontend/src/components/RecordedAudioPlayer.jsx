import { useEffect, useRef, useState } from 'react'
import { colors } from '../../utils/styles'
import { formatDuration, formatFileSize } from '../../utils/formatters'

/**
 * RecordedAudioPlayer — shows after recording stops.
 *
 * Lets the user preview their recording before sending it to
 * the STT backend. Includes:
 *  - Native <audio> element for playback
 *  - File size display
 *  - Duration from the recorder
 */
export default function RecordedAudioPlayer({ audioBlob, duration }) {
  const [audioUrl, setAudioUrl] = useState(null)

  useEffect(() => {
    if (!audioBlob) return
    const url = URL.createObjectURL(audioBlob)
    setAudioUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [audioBlob])

  if (!audioBlob || !audioUrl) return null

  return (
    <div style={{
      padding: '14px 16px',
      background: colors.gray[50],
      border: `1px solid ${colors.gray[200]}`,
      borderRadius: 10,
    }}>
      <div style={{
        display: 'flex', justifyContent: 'space-between',
        alignItems: 'center', marginBottom: 10,
      }}>
        <span style={{ fontSize: 13, fontWeight: 600, color: colors.gray[700] }}>
          🎙 Recording preview
        </span>
        <span style={{ fontSize: 12, color: colors.gray[400] }}>
          {formatDuration(duration)} · {formatFileSize(audioBlob.size)}
        </span>
      </div>

      <audio
        src={audioUrl}
        controls
        style={{ width: '100%', height: 36 }}
        aria-label="Preview your recording before transcribing"
      />

      <p style={{ fontSize: 12, color: colors.gray[400], margin: '8px 0 0' }}>
        Listen back before transcribing — you can re-record if needed.
      </p>
    </div>
  )
}
