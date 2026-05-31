import { useRef, useEffect } from 'react'
import { colors } from '../../utils/styles'

/**
 * WaveformCanvas — draws the audio waveform using the Canvas 2D API.
 *
 * Receives waveformData (Float32Array of 128 samples, -1 to +1) from
 * useRecorder's Web Audio analyser every ~15ms.
 *
 * When not recording (idle/done) draws a flat centered line.
 * When recording draws the live time-domain signal.
 * The bar style adapts to active vs inactive state.
 *
 * Props:
 *   waveformData  Float32Array  — from useRecorder
 *   active        bool          — true while mic is live
 *   height        number        — canvas height in px (default 64)
 *   barColor      string        — CSS color for active bars
 *   idleColor     string        — CSS color for idle state
 */
export default function WaveformCanvas({
  waveformData,
  active     = false,
  height     = 64,
  barColor   = colors.primary,
  idleColor  = colors.gray[300],
}) {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')

    const W  = canvas.width
    const H  = canvas.height
    const cx = H / 2   // vertical center
    const BARS       = 48
    const BAR_GAP    = 2
    const BAR_WIDTH  = (W - BAR_GAP * (BARS - 1)) / BARS
    const MIN_HEIGHT = 2   // minimum bar height so idle isn't completely flat

    ctx.clearRect(0, 0, W, H)

    if (!active || !waveformData || waveformData.length === 0) {
      // ── Idle / not recording — flat dotted line ────────────────────────────
      ctx.fillStyle = idleColor
      for (let i = 0; i < BARS; i++) {
        const x = i * (BAR_WIDTH + BAR_GAP)
        ctx.beginPath()
        ctx.roundRect(x, cx - 1, BAR_WIDTH, 2, 1)
        ctx.fill()
      }
      return
    }

    // ── Active — draw time-domain amplitude bars ───────────────────────────
    const step     = Math.floor(waveformData.length / BARS)
    const halfH    = H / 2 - 4

    ctx.fillStyle = barColor

    for (let i = 0; i < BARS; i++) {
      // Average a small window around each sample for smoother look
      let sum = 0
      for (let j = 0; j < step; j++) {
        sum += Math.abs(waveformData[i * step + j] ?? 0)
      }
      const amplitude = sum / step

      // Map amplitude (0–1) to bar height with a min/max clamp
      const barH = Math.max(MIN_HEIGHT, amplitude * halfH * 2.5)

      const x = i * (BAR_WIDTH + BAR_GAP)
      const y = cx - barH / 2

      ctx.beginPath()
      ctx.roundRect(x, y, BAR_WIDTH, barH, BAR_WIDTH / 2)
      ctx.fill()
    }

  }, [waveformData, active, barColor, idleColor])

  // Re-draw on window resize
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ro = new ResizeObserver(() => {
      canvas.width  = canvas.offsetWidth
      canvas.height = height
    })
    ro.observe(canvas)
    return () => ro.disconnect()
  }, [height])

  return (
    <canvas
      ref={canvasRef}
      aria-hidden="true"
      style={{
        width: '100%',
        height,
        display: 'block',
        borderRadius: 8,
      }}
    />
  )
}
