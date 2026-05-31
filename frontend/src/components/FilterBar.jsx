import { colors } from '../../utils/styles'

const STATUSES  = ['', 'done', 'failed', 'processing', 'pending']
const LANGUAGES = ['', 'en-US', 'en-GB', 'hi-IN', 'es-ES', 'fr-FR', 'de-DE', 'ja-JP']
const SORTS     = [
  { value: 'createdAt:desc',       label: 'Newest first'    },
  { value: 'createdAt:asc',        label: 'Oldest first'    },
  { value: 'wordCount:desc',       label: 'Most words'      },
  { value: 'durationSeconds:desc', label: 'Longest audio'   },
  { value: 'confidenceScore:desc', label: 'Best confidence' },
]

const STATUS_LABELS = {
  '': 'All statuses', done: 'Done', failed: 'Failed',
  processing: 'Processing', pending: 'Pending',
}

const LANG_LABELS = {
  '': 'All languages', 'en-US': 'English (US)', 'en-GB': 'English (UK)',
  'hi-IN': 'Hindi', 'es-ES': 'Spanish', 'fr-FR': 'French',
  'de-DE': 'German', 'ja-JP': 'Japanese',
}

/**
 * FilterBar — a responsive row of filter controls.
 *
 * Props:
 *   filters    { status, language, sortBy, sortDir }
 *   onChange   (patch) => void — called with partial filter updates
 *   total      number — shown as "N transcriptions"
 */
export default function FilterBar({ filters, onChange, total = 0 }) {
  const sortValue = `${filters.sortBy ?? 'createdAt'}:${filters.sortDir ?? 'desc'}`

  const handleSort = (val) => {
    const [sortBy, sortDir] = val.split(':')
    onChange({ sortBy, sortDir })
  }

  const activeFilters = [filters.status, filters.language].filter(Boolean).length

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      flexWrap: 'wrap',
    }}>

      {/* Result count */}
      <span style={{
        fontSize: 13, color: colors.gray[500],
        marginRight: 4, whiteSpace: 'nowrap',
      }}>
        {total} transcript{total !== 1 ? 's' : ''}
      </span>

      {/* Status filter */}
      <select
        value={filters.status ?? ''}
        onChange={e => onChange({ status: e.target.value || null })}
        aria-label="Filter by status"
        style={selectStyle(!!filters.status)}
      >
        {STATUSES.map(s => (
          <option key={s} value={s}>{STATUS_LABELS[s]}</option>
        ))}
      </select>

      {/* Language filter */}
      <select
        value={filters.language ?? ''}
        onChange={e => onChange({ language: e.target.value || null })}
        aria-label="Filter by language"
        style={selectStyle(!!filters.language)}
      >
        {LANGUAGES.map(l => (
          <option key={l} value={l}>{LANG_LABELS[l] ?? l}</option>
        ))}
      </select>

      {/* Sort */}
      <select
        value={sortValue}
        onChange={e => handleSort(e.target.value)}
        aria-label="Sort by"
        style={selectStyle(false)}
      >
        {SORTS.map(({ value, label }) => (
          <option key={value} value={value}>{label}</option>
        ))}
      </select>

      {/* Clear filters */}
      {activeFilters > 0 && (
        <button
          onClick={() => onChange({ status: null, language: null })}
          style={{
            fontSize: 12, padding: '5px 10px',
            background: '#FEF2F2',
            border: `1px solid #FECACA`,
            borderRadius: 7,
            color: colors.danger,
            cursor: 'pointer',
            fontFamily: 'inherit',
            whiteSpace: 'nowrap',
          }}
        >
          × Clear {activeFilters} filter{activeFilters > 1 ? 's' : ''}
        </button>
      )}
    </div>
  )
}

const selectStyle = (active) => ({
  padding: '6px 10px',
  fontSize: 13,
  border: `1px solid ${active ? colors.primaryMid : colors.gray[200]}`,
  borderRadius: 8,
  background: active ? colors.primaryLight : '#fff',
  color: active ? colors.primaryDark : colors.gray[700],
  cursor: 'pointer',
  fontFamily: 'inherit',
  outline: 'none',
})
