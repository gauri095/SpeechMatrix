import { useState, useRef, useCallback, useEffect } from 'react'

/**
 * useRecorder — wraps the browser's MediaRecorder API with waveform analysis.
 *
 * Returns everything the UI needs:
 *   recording    bool     — mic is currently capturing
 *   duration     number   — seconds elapsed
 *   audioBlob    Blob     — set after stop(), null before
 *   mimeType     string   — detected MIME e.g. "audio/webm;codecs=opus"
 *   waveformData Float32Array — 128-sample amplitude snapshot, updated ~15fps
 *   hasPermission bool    — null=unknown, true=granted, false=denied
 *   error        string   — human-readable mic error
 *   start()      async    — request permission + start recording
 *   stop()               — stop recording, audioBlob becomes available
 *   pause() / resume()   — for future use
 *   reset()              — clear blob, return to fresh state
 *
 * MIME type priority:
 *   audio/webm;codecs=opus  (Chrome, Edge, Brave)
 *   audio/ogg;codecs=opus   (Firefox)
 *   audio/mp4               (Safari 14.1+)
 *   '' (browser default)    (everything else)
 */
export function useRecorder() {
  const [recording,     setRecording]     = useState(false)
  const [paused,        setPaused]        = useState(false)
  const [duration,      setDuration]      = useState(0)
  const [audioBlob,     setAudioBlob]     = useState(null)
  const [mimeType,      setMimeType]      = useState('')
  const [waveformData,  setWaveformData]  = useState(new Float32Array(128))
  const [hasPermission, setHasPermission] = useState(null)   // null | true | false
  const [error,         setError]         = useState(null)

  // Refs — stable across renders, no stale closures
  const mediaRecorderRef = useRef(null)
  const chunksRef        = useRef([])
  const timerRef         = useRef(null)
  const streamRef        = useRef(null)
  const analyserRef      = useRef(null)
  const audioCtxRef      = useRef(null)
  const animFrameRef     = useRef(null)

  // ── Detect supported MIME ──────────────────────────────────────────────────
  const getSupportedMime = () => {
    const candidates = [
      'audio/webm;codecs=opus',
      'audio/webm',
      'audio/ogg;codecs=opus',
      'audio/ogg',
      'audio/mp4',
    ]
    if (typeof MediaRecorder === 'undefined') return ''
    return candidates.find(t => MediaRecorder.isTypeSupported(t)) ?? ''
  }

  // ── Waveform loop — runs at ~15 fps via requestAnimationFrame ──────────────
  const startWaveformLoop = useCallback((stream) => {
    try {
      const ctx      = new (window.AudioContext || window.webkitAudioContext)()
      const source   = ctx.createMediaStreamSource(stream)
      const analyser = ctx.createAnalyser()
      analyser.fftSize = 256   // 128 frequency bins
      analyser.smoothingTimeConstant = 0.8
      source.connect(analyser)

      audioCtxRef.current  = ctx
      analyserRef.current  = analyser

      const data = new Float32Array(analyser.frequencyBinCount)

      let lastFrame = 0
      const FRAME_INTERVAL = 1000 / 15   // ~15 fps

      const loop = (timestamp) => {
        animFrameRef.current = requestAnimationFrame(loop)
        if (timestamp - lastFrame < FRAME_INTERVAL) return
        lastFrame = timestamp
        analyser.getFloatTimeDomainData(data)
        // Copy so React detects a reference change
        setWaveformData(new Float32Array(data))
      }
      animFrameRef.current = requestAnimationFrame(loop)
    } catch {
      // AudioContext unavailable — waveform just stays flat; recording still works
    }
  }, [])

  const stopWaveformLoop = useCallback(() => {
    if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current)
    audioCtxRef.current?.close().catch(() => {})
    audioCtxRef.current  = null
    analyserRef.current  = null
    setWaveformData(new Float32Array(128))
  }, [])

  // ── start() ───────────────────────────────────────────────────────────────
  const start = useCallback(async () => {
    setError(null)

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation:  true,
          noiseSuppression:  true,
          sampleRate:        16000,   // optimal for most STT models
        }
      })
      streamRef.current = stream
      setHasPermission(true)

      const mime = getSupportedMime()
      setMimeType(mime)
      chunksRef.current = []

      const recorder = new MediaRecorder(
        stream,
        mime ? { mimeType: mime } : {}
      )
      mediaRecorderRef.current = recorder

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data)
      }

      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: mime || 'audio/webm' })
        setAudioBlob(blob)
        streamRef.current?.getTracks().forEach(t => t.stop())
        stopWaveformLoop()
      }

      // Collect in 250ms chunks — better for large recordings
      recorder.start(250)
      setRecording(true)
      setPaused(false)
      setDuration(0)
      setAudioBlob(null)

      // Start waveform analyser
      startWaveformLoop(stream)

      // Start timer
      timerRef.current = setInterval(() => {
        setDuration(d => d + 1)
      }, 1000)

    } catch (err) {
      setHasPermission(false)
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        setError('Microphone permission denied. Click the mic icon in your browser address bar to allow access.')
      } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
        setError('No microphone detected. Please connect a microphone and try again.')
      } else if (err.name === 'NotReadableError') {
        setError('Microphone is already in use by another application.')
      } else {
        setError(`Could not access microphone: ${err.message}`)
      }
    }
  }, [startWaveformLoop, stopWaveformLoop])

  // ── stop() ────────────────────────────────────────────────────────────────
  const stop = useCallback(() => {
    clearInterval(timerRef.current)
    mediaRecorderRef.current?.stop()
    setRecording(false)
    setPaused(false)
  }, [])

  // ── pause() / resume() ────────────────────────────────────────────────────
  const pause = useCallback(() => {
    if (mediaRecorderRef.current?.state === 'recording') {
      mediaRecorderRef.current.pause()
      clearInterval(timerRef.current)
      setPaused(true)
    }
  }, [])

  const resume = useCallback(() => {
    if (mediaRecorderRef.current?.state === 'paused') {
      mediaRecorderRef.current.resume()
      timerRef.current = setInterval(() => setDuration(d => d + 1), 1000)
      setPaused(false)
    }
  }, [])

  // ── reset() ───────────────────────────────────────────────────────────────
  const reset = useCallback(() => {
    stop()
    setAudioBlob(null)
    setDuration(0)
    setError(null)
    setPaused(false)
    setWaveformData(new Float32Array(128))
    chunksRef.current = []
  }, [stop])

  // ── Cleanup on unmount ────────────────────────────────────────────────────
  useEffect(() => {
    return () => {
      clearInterval(timerRef.current)
      stopWaveformLoop()
      streamRef.current?.getTracks().forEach(t => t.stop())
    }
  }, [stopWaveformLoop])

  return {
    recording, paused, duration, audioBlob,
    mimeType, waveformData, hasPermission, error,
    start, stop, pause, resume, reset,
  }
}