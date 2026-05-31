import { useState, useCallback } from 'react'
import { useRecorder } from './useRecorder'
import speechService from '../services/speechService'

/**
 * useRecordAndTranscribe — combines useRecorder with backend submission.
 *
 * States:
 *   idle        → ready to record
 *   recording   → mic active, capturing audio
 *   paused      → recording paused
 *   recorded    → blob ready, not yet sent
 *   submitting  → blob sent to backend, STT in progress
 *   done        → transcript result ready
 *   error       → submission failed
 *
 * Usage:
 *   const rec = useRecordAndTranscribe()
 *   <button onClick={rec.start}>Record</button>
 *   <button onClick={rec.stop}>Stop</button>
 *   <button onClick={rec.submit}>Transcribe</button>
 */
export function useRecordAndTranscribe() {
  const recorder = useRecorder()

  const [submitState, setSubmitState] = useState('idle')  // idle|submitting|done|error
  const [result,      setResult]      = useState(null)
  const [submitError, setSubmitError] = useState(null)
  const [language,    setLanguage]    = useState('en-US')

  // ── Derived state ──────────────────────────────────────────────────────────
  const phase = recorder.recording ? 'recording'
              : recorder.paused    ? 'paused'
              : recorder.audioBlob && submitState === 'idle'   ? 'recorded'
              : submitState === 'submitting' ? 'submitting'
              : submitState === 'done'       ? 'done'
              : submitState === 'error'      ? 'error'
              : 'idle'

  // ── Stop and keep blob ─────────────────────────────────────────────────────
  const stopRecording = useCallback(() => {
    recorder.stop()
    // submitState stays 'idle' — blob will be available shortly
  }, [recorder])

  // ── Submit blob to backend ─────────────────────────────────────────────────
  const submit = useCallback(async () => {
    if (!recorder.audioBlob) return
    setSubmitState('submitting')
    setSubmitError(null)

    try {
      const transcript = await speechService.record(
        recorder.audioBlob,
        language,
        recorder.mimeType || 'audio/webm'
      )
      setResult(transcript)
      setSubmitState('done')
    } catch (err) {
      setSubmitState('error')
      setSubmitError(err.apiMessage ?? 'Transcription failed. Please try again.')
    }
  }, [recorder.audioBlob, recorder.mimeType, language])

  // ── Reset everything ───────────────────────────────────────────────────────
  const reset = useCallback(() => {
    recorder.reset()
    setSubmitState('idle')
    setResult(null)
    setSubmitError(null)
  }, [recorder])

  // ── Retry same blob ────────────────────────────────────────────────────────
  const retry = useCallback(() => {
    setSubmitState('idle')
    setSubmitError(null)
  }, [])

  return {
    // Recorder pass-throughs
    recording:    recorder.recording,
    paused:       recorder.paused,
    duration:     recorder.duration,
    audioBlob:    recorder.audioBlob,
    waveformData: recorder.waveformData,
    hasPermission:recorder.hasPermission,
    micError:     recorder.error,

    // Phase + submission
    phase,
    result,
    submitError,
    language, setLanguage,

    // Actions
    start:  recorder.start,
    stop:   stopRecording,
    pause:  recorder.pause,
    resume: recorder.resume,
    submit,
    reset,
    retry,

    // Convenience booleans
    isIdle:       phase === 'idle',
    isRecording:  phase === 'recording',
    isPaused:     phase === 'paused',
    isRecorded:   phase === 'recorded',
    isSubmitting: phase === 'submitting',
    isDone:       phase === 'done',
    isError:      phase === 'error',
    isBusy:       phase === 'submitting',
  }
}