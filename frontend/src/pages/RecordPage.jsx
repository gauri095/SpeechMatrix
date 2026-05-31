import AppLayout    from '../components/layout/AppLayout'
import AudioRecorder from '../components/speech/AudioRecorder'
import { colors }   from '../utils/styles'

export default function RecordPage() {
  return (
    <AppLayout>
      <div style={{ maxWidth: 600, margin: '0 auto' }}>

        <div style={{ marginBottom: 28 }}>
          <h1 style={{
            fontSize: 24, fontWeight: 700, color: colors.gray[900],
            letterSpacing: '-0.02em', margin: '0 0 6px',
          }}>
            Record audio
          </h1>
          <p style={{ fontSize: 14, color: colors.gray[500], margin: 0 }}>
            Use your microphone to record and transcribe speech in real time.
          </p>
        </div>

        <div style={{
          background: '#fff',
          border: `1px solid ${colors.gray[200]}`,
          borderRadius: 16,
          padding: 24,
        }}>
          <AudioRecorder />
        </div>

        <div style={{
          marginTop: 20, padding: '14px 16px',
          background: colors.gray[50],
          border: `1px solid ${colors.gray[200]}`,
          borderRadius: 10,
        }}>
          <p style={{ fontSize: 13, fontWeight: 600, color: colors.gray[700], margin: '0 0 8px' }}>
            💡 Tips for best results
          </p>
          <ul style={{
            fontSize: 13, color: colors.gray[500], margin: 0,
            paddingLeft: 18, lineHeight: 1.7,
          }}>
            <li>Speak clearly and at a normal pace</li>
            <li>Reduce background noise — close windows, turn off fans</li>
            <li>Hold the device 15–30 cm from your mouth</li>
            <li>Select the correct language before recording</li>
          </ul>
        </div>
      </div>
    </AppLayout>
  )
}