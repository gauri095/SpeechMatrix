import { useState, useCallback, useRef } from 'react'
import speechService from '../services/speechService'

/**
 * useUpload — manages the full lifecycle of an audio file upload.
 *
 * States:
 *   idle        → no file selected yet
 *   selected    → file chosen, not yet uploading
 *   uploading   → bytes transferring to server (progress 0–100)
 *   processing  → file received, STT provider working
 *   done        → transcript ready
 *   error       → something failed
 *
 * The split between "uploading" and "processing" matters UX-wise:
 * uploading finishes at 100% almost instantly for small files,
 * but the STT API may take 5–30 seconds to return the transcript.
 * Showing "Processing audio…" instead of a stuck progress bar
 * prevents the user thinking the app has frozen.
 */
export function useUpload() {
  const [state,      setState]    = useState('idle')   // idle|selected|uploading|processing|done|error
  const [file,       setFile]     = useState(null)
  const [progress,   setProgress] = useState(0)
  const [result,     setResult]   = useState(null)    // TranscriptionDto.Response
  const [error,      setError]    = useState(null)
  const [options,    setOptions]  = useState({ language: 'en-US', speakerCount: null })
  const abortRef = useRef(null)

  // ── Select a file ─────────────────────────────────────────────────────────
  const selectFile = useCallback((f) => {
    if (!f) return
    setFile(f)
    setState('selected')
    setError(null)
    setResult(null)
    setProgress(0)
  }, [])

  // ── Update options ────────────────────────────────────────────────────────
  const updateOptions = useCallback((patch) => {
    setOptions(prev => ({ ...prev, ...patch }))
  }, [])

  // ── Start upload ──────────────────────────────────────────────────────────
  const upload = useCallback(async () => {
    if (!file) return
    setState('uploading')
    setProgress(0)
    setError(null)

    try {
      const transcript = await speechService.upload(
        file,
        options.language,
        options.speakerCount,
        (pct) => {
          setProgress(pct)
          // Once the bytes are fully transferred, flip to "processing"
          if (pct >= 100) setState('processing')
        }
      )
      setResult(transcript)
      setState('done')
    } catch (err) {
      setState('error')
      setError(err.apiMessage ?? 'Upload failed. Please try again.')
    }
  }, [file, options])

  // ── Reset everything ──────────────────────────────────────────────────────
  const reset = useCallback(() => {
    setState('idle')
    setFile(null)
    setProgress(0)
    setResult(null)
    setError(null)
  }, [])

  // ── Remove just the file (keep options) ───────────────────────────────────
  const removeFile = useCallback(() => {
    setFile(null)
    setState('idle')
    setError(null)
  }, [])

  return {
    state, file, progress, result, error, options,
    selectFile, updateOptions, upload, reset, removeFile,
    isIdle:       state === 'idle',
    isSelected:   state === 'selected',
    isUploading:  state === 'uploading',
    isProcessing: state === 'processing',
    isDone:       state === 'done',
    isError:      state === 'error',
    isBusy:       state === 'uploading' || state === 'processing',
  }
}