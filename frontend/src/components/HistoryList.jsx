import { useLink } from 'react-router-dom'
import { Link } from 'react-router-dom'
import TranscriptCard from './TranscriptCard'
import Spinner from '../common/Spinner'
import Button  from '../common/Button'
import { colors } from '../../utils/styles'

/**
 * HistoryList — renders the transcript cards.
 *
 * Handles three states:
 *   loading (initial) — skeleton shimmer
 *   empty             — friendly empty state with CTA
 *   populated         — list of TranscriptCard items + Load More
 */
export default function HistoryList({
  items,
  loading,
  hasMore,
  onLoadMore,
  onDelete,
  loadingMore = false,
  searchActive = false,
  searchQuery  = '',
}) {
  // ── Initial loading ─────────────────────────────────────────────────────────
  if (loading && items.length === 0) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {[1, 2, 3].map(i => <SkeletonCard key={i} />)}
      </div>
    )
  }

  // ── Empty state ─────────────────────────────────────────────────────────────
  if (!loading && items.length === 0) {
    return (
      <div style={{
        textAlign: 'center',
        padding: '60px 24px',
        background: '#fff',
        border: `1px solid ${colors.gray[200]}`,
        borderRadius: 12,
      }}>
        <div style={{ fontSize: 48, marginBottom: 12 }}>
          {searchActive ? '🔍' : '🎙'}
        </div>
        <p style={{ fontSize: 16, fontWeight: 600, color: colors.gray[700], margin: '0 0 6px' }}>
          {searchActive
            ? `No results for "${searchQuery}"`
            : 'No transcriptions yet'}
        </p>
        <p style={{ fontSize: 13, color: colors.gray[400], margin: '0 0 20px' }}>
          {searchActive
            ? 'Try a different search term or clear the search'
            : 'Upload an audio file or record your voice to get started'}
        </p>
        {!searchActive && (
          <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
            <Link to="/upload" style={{
              padding: '8px 18px', fontSize: 13, fontWeight: 500,
              background: colors.primary, color: '#fff',
              borderRadius: 8, textDecoration: 'none',
            }}>
              Upload file
            </Link>
            <Link to="/record" style={{
              padding: '8px 18px', fontSize: 13, fontWeight: 500,
              background: '#fff', color: colors.gray[700],
              border: `1px solid ${colors.gray[200]}`,
              borderRadius: 8, textDecoration: 'none',
            }}>
              Record audio
            </Link>
          </div>
        )}
      </div>
    )
  }

  // ── List ────────────────────────────────────────────────────────────────────
  return (
    <div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {items.map(item => (
          <TranscriptCard
            key={item.id}
            item={item}
            onDelete={onDelete}
            highlighted={!!item.highlightedPreview}
          />
        ))}
      </div>

      {/* Load more */}
      {hasMore && (
        <div style={{ marginTop: 20, textAlign: 'center' }}>
          <Button
            variant="secondary"
            onClick={onLoadMore}
            loading={loadingMore}
            style={{ minWidth: 160 }}
          >
            {loadingMore ? 'Loading…' : 'Load more'}
          </Button>
        </div>
      )}

      {/* End of list */}
      {!hasMore && items.length > 0 && (
        <p style={{
          textAlign: 'center', fontSize: 12,
          color: colors.gray[300], margin: '20px 0 0',
        }}>
          — {items.length} transcript{items.length !== 1 ? 's' : ''} total —
        </p>
      )}
    </div>
  )
}

// ── Skeleton shimmer card ─────────────────────────────────────────────────────
function SkeletonCard() {
  return (
    <div style={{
      background: '#fff',
      border: `1px solid ${colors.gray[100]}`,
      borderRadius: 12,
      padding: '16px 18px',
      overflow: 'hidden',
    }}>
      <style>{`
        @keyframes shimmer {
          0%   { background-position: -200% 0; }
          100% { background-position:  200% 0; }
        }
        .sk {
          background: linear-gradient(
            90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%
          );
          background-size: 200% 100%;
          animation: shimmer 1.4s ease-in-out infinite;
          border-radius: 4px;
        }
      `}</style>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
        <div className="sk" style={{ width: 52, height: 18 }} />
        <div className="sk" style={{ width: 80, height: 14 }} />
      </div>
      <div className="sk" style={{ width: '90%', height: 14, marginBottom: 6 }} />
      <div className="sk" style={{ width: '70%', height: 14, marginBottom: 14 }} />
      <div style={{ display: 'flex', gap: 10 }}>
        {[60, 80, 70, 60].map((w, i) => (
          <div key={i} className="sk" style={{ width: w, height: 12 }} />
        ))}
      </div>
    </div>
  )
}
