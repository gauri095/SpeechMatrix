import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { colors } from '../../utils/styles'
import {
  formatDate, formatDuration, formatWordCount,
  formatConfidence, getLanguageName, getStatusBadge,
} from '../../utils/formatters'

/**
 * TranscriptCard — one item in the history list.
 *
 * Shows:
 *  - Status badge
 *  - Transcript preview (120 chars, truncated)
 *  - Metadata strip: date, duration, words, confidence, language
 *  - Hover actions: View, Delete
 *
 * The `highlightedPreview` prop (from search results) renders raw HTML
 * so the <mark> tag wrapping the matched term displays correctly.
 */
export default function TranscriptCard({ item, onDelete, highlighted = false }) {
  const navigate    = useNavigate()
  const [hover, setHover] = useState(false)
  const badge = getStatusBadge(item.status)

  const BADGE_COLORS = {
    green:  { bg: '#DCFCE7', color: '#166534' },
    red:    { bg: '#FEE2E2', color: '#991B1B' },
    yellow: { bg: '#FEF9C3', color: '#854D0E' },
    gray:   { bg: colors.gray[100], color: colors.gray[600] },
  }
  const bc = BADGE_COLORS[badge.color] ?? BADGE_COLORS.gray

  const preview = item.transcriptPreview ?? item.transcript?.slice(0, 120)

  return (
    <div
      role="article"
      onClick={() => navigate(`/history/${item.id}`)}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      style={{
        background: '#fff',
        border: `1px solid ${hover ? colors.primaryMid : colors.gray[200]}`,
        borderRadius: 12,
        padding: '16px 18px',
        cursor: 'pointer',
        transition: 'border-color 0.15s, box-shadow 0.15s',
        boxShadow: hover ? `0 2px 12px ${colors.primaryLight}` : 'none',
        outline: highlighted ? `2px solid ${colors.primary}` : 'none',
      }}
    >
      {/* Top row: status badge + date */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
        <span style={{
          fontSize: 11, fontWeight: 600,
          padding: '2px 8px', borderRadius: 20,
          background: bc.bg, color: bc.color,
        }}>
          {badge.label}
        </span>
        <span style={{ fontSize: 12, color: colors.gray[400] }}>
          {formatDate(item.createdAt)}
        </span>
      </div>

      {/* Transcript preview */}
      <div style={{
        fontSize: 14,
        color: item.status === 'failed' ? colors.gray[400] : colors.gray[700],
        lineHeight: 1.6,
        marginBottom: 12,
        minHeight: 22,
      }}>
        {item.highlightedPreview ? (
          /* Search result — render HTML <mark> tags */
          <span dangerouslySetInnerHTML={{ __html: item.highlightedPreview }} />
        ) : preview ? (
          <span>{preview}{(preview?.length ?? 0) >= 120 ? '…' : ''}</span>
        ) : (
          <em style={{ color: colors.gray[300] }}>
            {item.status === 'failed' ? 'Transcription failed' : 'No preview available'}
          </em>
        )}
      </div>

      {/* Metadata strip + actions */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 6 }}>

        {/* Metadata chips */}
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {[
            item.durationSeconds && formatDuration(item.durationSeconds),
            item.wordCount && formatWordCount(item.wordCount),
            item.confidenceScore && formatConfidence(item.confidenceScore),
            item.language && getLanguageName(item.language),
          ].filter(Boolean).map((label, i) => (
            <span key={i} style={{ fontSize: 12, color: colors.gray[400] }}>
              {label}
            </span>
          ))}
        </div>

        {/* Action buttons — visible on hover */}
        {hover && (
          <div
            style={{ display: 'flex', gap: 6 }}
            onClick={e => e.stopPropagation()}   // don't navigate on button click
          >
            <ActionBtn
              label="View"
              onClick={() => navigate(`/history/${item.id}`)}
            />
            {onDelete && (
              <ActionBtn
                label="Delete"
                danger
                onClick={() => onDelete(item)}
              />
            )}
          </div>
        )}
      </div>
    </div>
  )
}

function ActionBtn({ label, onClick, danger = false }) {
  return (
    <button
      onClick={onClick}
      style={{
        fontSize: 12, padding: '4px 10px',
        background: danger ? '#FEF2F2' : colors.gray[50],
        border: `1px solid ${danger ? '#FECACA' : colors.gray[200]}`,
        borderRadius: 7,
        color: danger ? colors.danger : colors.gray[600],
        cursor: 'pointer',
        fontFamily: 'inherit',
        fontWeight: 500,
      }}
    >
      {label}
    </button>
  )
}
