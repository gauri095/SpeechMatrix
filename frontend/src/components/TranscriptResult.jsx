import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { colors, card } from '../../utils/styles'
import {
  formatDuration, formatConfidence, formatWordCount,
  formatDate, getLanguageName
} from '../../utils/formatters'
import Button from '../common/Button'

/**
 * TranscriptResult — shown after a successful transcription.
 *
 * Displays:
 *   - Metadata strip (duration, words, confidence, language)
 *   - Full transcript text in a scrollable box
 *   - Copy to clipboard button
 *   - Download as .txt button
 *   - "View in history" link
 *   - "Upload another" button
 */
export default function TranscriptResult({ result, onUploadAnother }) {
  const navigate      = useNavigate()
  const [copied, setCopied] = useState(false)

  if (!result) return null

  // ── Copy ───────────────────────────────────────────────────────────────────
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(result.transcript ?? '')
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // Clipboard API unavailable — select the text manually
      const el = document.getElementById('transcript-text')
      if (el) {
        const range = document.createRange()
        range.selectNode(el)
        window.getSelection()?.removeAllRanges()
        window.getSelection()?.addRange(range)
      }
    }
  }

  // ── Download ───────────────────────────────────────────────────────────────
  const handleDownload = () => {
    const blob = new Blob([result.transcript ?? ''], { type: 'text/plain;charset=utf-8' })
    const url  = URL.createObjectURL(blob)
    const a    = document.createElement('a')
    a.href     = url
    a.download = `transcript-${result.id}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div>
      {/* Success banner */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '12px 16px', marginBottom: 16,
        background: colors.primaryLight,
        border: `1px solid ${colors.primaryMid}`,
        borderRadius: 10,
      }}>
        <span style={{ fontSize: 20 }}>✅</span>
        <div>
          <p style={{ fontWeight: 600, fontSize: 14, color: colors.primaryDark, margin: 0 }}>
            Transcription complete
          </p>
          <p style={{ fontSize: 12, color: colors.gray[500], margin: '2px 0 0' }}>
            Saved to your history · {formatDate(result.createdAt)}
          </p>
        </div>
      </div>

      {/* Metadata strip */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
        gap: 8, marginBottom: 16,
      }}>
        {[
          { icon: '⏱', label: 'Duration',   value: formatDuration(result.durationSeconds) },
          { icon: '📝', label: 'Words',      value: formatWordCount(result.wordCount) },
          { icon: '🎯', label: 'Confidence', value: formatConfidence(result.confidenceScore) },
          { icon: '🌐', label: 'Language',   value: getLanguageName(result.language) },
          ...(result.speakerCount > 1
            ? [{ icon: '👥', label: 'Speakers', value: `${result.speakerCount} speakers` }]
            : []),
        ].map(({ icon, label, value }) => (
          <div key={label} style={{
            ...card,
            padding: '10px 14px',
            background: colors.gray[50],
          }}>
            <p style={{ fontSize: 11, color: colors.gray[400], margin: '0 0 2px' }}>
              {icon} {label}
            </p>
            <p style={{ fontSize: 14, fontWeight: 600, color: colors.gray[800], margin: 0 }}>
              {value}
            </p>
          </div>
        ))}
      </div>

      {/* Transcript box */}
      <div style={{
        ...card,
        padding: 0,
        overflow: 'hidden',
        marginBottom: 16,
      }}>
        {/* Box header */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '12px 16px',
          borderBottom: `1px solid ${colors.gray[100]}`,
          background: colors.gray[50],
        }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: colors.gray[700] }}>
            Transcript
          </span>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={handleCopy}
              style={{
                fontSize: 12, padding: '4px 10px',
                background: 'none',
                border: `1px solid ${colors.gray[200]}`,
                borderRadius: 6,
                color: copied ? colors.primary : colors.gray[600],
                cursor: 'pointer',
                fontWeight: copied ? 600 : 400,
                transition: 'color 0.2s',
              }}
            >
              {copied ? '✓ Copied' : '⎘ Copy'}
            </button>
            <button
              onClick={handleDownload}
              style={{
                fontSize: 12, padding: '4px 10px',
                background: 'none',
                border: `1px solid ${colors.gray[200]}`,
                borderRadius: 6,
                color: colors.gray[600],
                cursor: 'pointer',
              }}
            >
              ↓ Download .txt
            </button>
          </div>
        </div>

        {/* Text */}
        <div
          id="transcript-text"
          style={{
            padding: '16px',
            maxHeight: 360,
            overflowY: 'auto',
            fontSize: 14,
            lineHeight: 1.75,
            color: colors.gray[800],
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
          }}
        >
          {result.transcript || (
            <em style={{ color: colors.gray[400] }}>
              No transcript text returned.
            </em>
          )}
        </div>
      </div>

      {/* Action row */}
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <Button
          variant="secondary"
          onClick={() => navigate(`/history/${result.id}`)}
          style={{ flex: 1, minWidth: 160 }}
        >
          View full transcript →
        </Button>
        <Button
          variant="primary"
          onClick={onUploadAnother}
          style={{ flex: 1, minWidth: 160 }}
        >
          Upload another file
        </Button>
      </div>
    </div>
  )
}
