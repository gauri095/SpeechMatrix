import { useState } from 'react'
import { colors } from '../../utils/styles'
import {
  formatDate, formatDuration, formatWordCount,
  formatConfidence, getLanguageName, getStatusBadge,
} from '../../utils/formatters'

/**
 * TranscriptViewer — the full transcript detail view.
 *
 * Used both in TranscriptPage (standalone) and as an expandable
 * panel in the history list.
 *
 * Features:
 *  - Metadata grid
 *  - Full scrollable transcript text
 *  - Copy to clipboard (with fallback)
 *  - Download as .txt
 *  - Download as .srt (simple subtitle file) when timestamps available
 */
export default function TranscriptViewer({ transcript }) {
  const [copied, setCopied] = useState(false)

  if (!transcript) return null

  const badge = getStatusBadge(transcript.status)

  // ── Copy ──────────────────────────────────────────────────────────────────
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(transcript.transcript ?? '')
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      const sel = window.getSelection()
      const el  = document.getElementById('tx-text')
      if (el && sel) {
        const range = document.createRange()
        range.selectNodeContents(el)
        sel.removeAllRanges()
        sel.addRange(range)
      }
    }
  }

  // ── Download .txt ─────────────────────────────────────────────────────────
  const handleDownloadTxt = () => {
    const content = [
      `VoxScript Transcript`,
      `Date: ${new Date(transcript.createdAt).toLocaleString()}`,
      `Language: ${getLanguageName(transcript.language)}`,
      `Duration: ${formatDuration(transcript.durationSeconds)}`,
      `Words: ${transcript.wordCount ?? 0}`,
      `Confidence: ${formatConfidence(transcript.confidenceScore)}`,
      `\n${'─'.repeat(60)}\n`,
      transcript.transcript ?? '',
    ].join('\n')

    downloadText(content, `transcript-${transcript.id}.txt`, 'text/plain')
  }

  // ── Download .srt (subtitle) ──────────────────────────────────────────────
  const handleDownloadSrt = () => {
    if (!transcript.transcript) return
    // Simple single-block SRT when no word timestamps available
    const durationSecs = transcript.durationSeconds ?? 60
    const srt = `1\n00:00:00,000 --> ${toSrtTime(durationSecs)}\n${transcript.transcript}\n`
    downloadText(srt, `transcript-${transcript.id}.srt`, 'text/plain')
  }

  const BADGE_COLORS = {
    green:  { bg: '#DCFCE7', color: '#166534' },
    red:    { bg: '#FEE2E2', color: '#991B1B' },
    yellow: { bg: '#FEF9C3', color: '#854D0E' },
    gray:   { bg: colors.gray[100], color: colors.gray[600] },
  }
  const bc = BADGE_COLORS[badge.color] ?? BADGE_COLORS.gray

  // ── Metadata items ────────────────────────────────────────────────────────
  const meta = [
    { icon: '📅', label: 'Date',       value: formatDate(transcript.createdAt) },
    { icon: '⏱',  label: 'Duration',   value: formatDuration(transcript.durationSeconds) },
    { icon: '📝', label: 'Words',      value: formatWordCount(transcript.wordCount) },
    { icon: '🎯', label: 'Confidence', value: formatConfidence(transcript.confidenceScore) },
    { icon: '🌐', label: 'Language',   value: getLanguageName(transcript.language) },
    { icon: '🤖', label: 'Provider',   value: transcript.sttProvider ?? '—' },
    ...(transcript.speakerCount > 1
      ? [{ icon: '👥', label: 'Speakers', value: `${transcript.speakerCount} speakers` }]
      : []),
  ]

  return (
    <div>
      {/* Header: status badge */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
        <span style={{
          fontSize: 12, fontWeight: 600,
          padding: '3px 10px', borderRadius: 20,
          background: bc.bg, color: bc.color,
        }}>
          {badge.label}
        </span>
        <span style={{ fontSize: 13, color: colors.gray[400] }}>
          ID #{transcript.id}
        </span>
      </div>

      {/* Metadata grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
        gap: 8,
        marginBottom: 20,
      }}>
        {meta.map(({ icon, label, value }) => (
          <div key={label} style={{
            padding: '10px 12px',
            background: colors.gray[50],
            border: `1px solid ${colors.gray[100]}`,
            borderRadius: 8,
          }}>
            <p style={{ fontSize: 11, color: colors.gray[400], margin: '0 0 2px' }}>
              {icon} {label}
            </p>
            <p style={{ fontSize: 13, fontWeight: 600, color: colors.gray[700], margin: 0 }}>
              {value}
            </p>
          </div>
        ))}
      </div>

      {/* Transcript box */}
      <div style={{
        border: `1px solid ${colors.gray[200]}`,
        borderRadius: 10,
        overflow: 'hidden',
        marginBottom: 16,
      }}>
        {/* Toolbar */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '10px 14px',
          background: colors.gray[50],
          borderBottom: `1px solid ${colors.gray[100]}`,
          flexWrap: 'wrap', gap: 8,
        }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: colors.gray[700] }}>
            Transcript
          </span>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {[
              { label: copied ? '✓ Copied' : '⎘ Copy',   onClick: handleCopy    },
              { label: '↓ .txt',                           onClick: handleDownloadTxt },
              { label: '↓ .srt',                           onClick: handleDownloadSrt },
            ].map(({ label, onClick }) => (
              <button key={label} onClick={onClick} style={{
                fontSize: 12, padding: '4px 10px',
                background: '#fff',
                border: `1px solid ${colors.gray[200]}`,
                borderRadius: 6,
                color: label.includes('Copied') ? colors.primary : colors.gray[600],
                cursor: 'pointer',
                fontFamily: 'inherit',
                fontWeight: label.includes('Copied') ? 600 : 400,
              }}>
                {label}
              </button>
            ))}
          </div>
        </div>

        {/* Text */}
        <div
          id="tx-text"
          style={{
            padding: '16px',
            fontSize: 14,
            lineHeight: 1.8,
            color: colors.gray[800],
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            maxHeight: 500,
            overflowY: 'auto',
          }}
        >
          {transcript.transcript || (
            <em style={{ color: colors.gray[300] }}>No transcript text available.</em>
          )}
        </div>
      </div>

      {/* AI summary — shown when available (Week 4) */}
      {transcript.summary && (
        <div style={{
          padding: '14px 16px',
          background: '#EFF6FF',
          border: '1px solid #BFDBFE',
          borderRadius: 10,
        }}>
          <p style={{ fontSize: 13, fontWeight: 600, color: '#1E40AF', margin: '0 0 6px' }}>
            ✨ AI Summary
          </p>
          <p style={{ fontSize: 13, color: '#1D4ED8', margin: 0, lineHeight: 1.6 }}>
            {transcript.summary}
          </p>
        </div>
      )}
    </div>
  )
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function downloadText(content, filename, type) {
  const blob = new Blob([content], { type })
  const url  = URL.createObjectURL(blob)
  const a    = document.createElement('a')
  a.href     = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function toSrtTime(seconds) {
  const h  = Math.floor(seconds / 3600)
  const m  = Math.floor((seconds % 3600) / 60)
  const s  = Math.floor(seconds % 60)
  const ms = Math.floor((seconds % 1) * 1000)
  return `${pad(h)}:${pad(m)}:${pad(s)},${String(ms).padStart(3,'0')}`
}

function pad(n) { return String(n).padStart(2, '0') }
