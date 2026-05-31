import { useRecordAndTranscribe } from '../../hooks/useRecordAndTranscribe'
import { useToast } from '../common/Toast'
import RecordButton        from '../RecordButton'
import RecordingControls   from '../RecordingControls'
import WaveformCanvas      from '../WaveformCanvas'
import RecordedAudioPlayer from '../RecordedAudioPlayer'
import PermissionBanner    from '../PermissionBanner'
import LanguagePicker      from '../LanguagePicker'
import TranscriptResult    from '../TranscriptResult'
import ErrorMessage        from '../common/ErrorMessage'
import Button              from '../common/Button'
import Spinner             from '../common/Spinner'
import { colors }          from '../../utils/styles'

/**
 * AudioRecorder — the complete recording experience.
 *
 * Phase flow:
 *   idle → recording → (pause ↔ resume) → recorded → submitting → done
 *                                         ↓ discard ↓
 *                                          idle
 */
export default function AudioRecorder() {
  const rec   = useRecordAndTranscribe()
  const toast = useToast()

  const handleStop = () => {
    rec.stop()
    toast.info('Recording stopped — press "Transcribe" when ready')
  }

  const handleDiscard = () => {
    rec.reset()
    toast.info('Recording discarded')
  }

  const handleSubmit = async () => {
    await rec.submit()
    if (rec.isDone) toast.success('Transcription complete!')
  }

  // ── Done — show full result ────────────────────────────────────────────────
  if (rec.isDone) {
    return <TranscriptResult result={rec.result} onUploadAnother={rec.reset} />
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* Language selector — shown when idle */}
      {rec.isIdle && (
        <LanguagePicker
          value={rec.language}
          onChange={rec.setLanguage}
        />
      )}

      {/* Permission / error banners */}
      <PermissionBanner
        hasPermission={rec.hasPermission}
        error={rec.micError}
      />

      {/* Main record button + waveform */}
      <div style={{
        padding: '32px 24px',
        background: colors.gray[50],
        border: `1px solid ${colors.gray[200]}`,
        borderRadius: 16,
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', gap: 24,
      }}>

        {/* Waveform — shows during recording, flat otherwise */}
        <div style={{ width: '100%', maxWidth: 400 }}>
          <WaveformCanvas
            waveformData={rec.waveformData}
            active={rec.isRecording}
            height={72}
          />
        </div>

        {/* The big mic button */}
        <RecordButton
          phase={rec.phase}
          duration={rec.duration}
          onStart={rec.start}
          onStop={handleStop}
          onPause={rec.pause}
          onResume={rec.resume}
          disabled={rec.isSubmitting}
        />

        {/* Pause / Discard controls */}
        <RecordingControls
          phase={rec.phase}
          onPause={rec.pause}
          onResume={rec.resume}
          onDiscard={handleDiscard}
        />
      </div>

      {/* Audio preview — shown after recording stops */}
      {rec.isRecorded && (
        <RecordedAudioPlayer
          audioBlob={rec.audioBlob}
          duration={rec.duration}
        />
      )}

      {/* Submission error */}
      {rec.isError && (
        <ErrorMessage
          message={rec.submitError}
          onRetry={rec.retry}
        />
      )}

      {/* Submitting indicator */}
      {rec.isSubmitting && (
        <div style={{
          padding: '16px',
          background: colors.primaryLight,
          border: `1px solid ${colors.primaryMid}`,
          borderRadius: 10,
          display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <Spinner size="sm" label="" />
          <div>
            <p style={{ fontWeight: 600, fontSize: 14, color: colors.primaryDark, margin: 0 }}>
              Processing audio…
            </p>
            <p style={{ fontSize: 12, color: colors.gray[500], margin: '2px 0 0' }}>
              Sending to speech recognition — this may take a moment
            </p>
          </div>
        </div>
      )}

      {/* Action row — shown after recording is ready */}
      {(rec.isRecorded || rec.isError) && rec.audioBlob && (
        <div style={{ display: 'flex', gap: 10 }}>
          <Button
            variant="secondary"
            onClick={handleDiscard}
            style={{ flex: 1 }}
          >
            🗑 Record again
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            loading={rec.isSubmitting}
            style={{ flex: 2 }}
          >
            🚀 Transcribe recording
          </Button>
        </div>
      )}

      {/* Hint text */}
      {rec.isIdle && !rec.micError && (
        <p style={{ fontSize: 13, color: colors.gray[400], textAlign: 'center', margin: 0 }}>
          Your browser will ask for microphone permission on first use.
        </p>
      )}
    </div>
  )
}
