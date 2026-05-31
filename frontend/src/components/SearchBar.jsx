import { useRef } from 'react'
import { colors } from '../../utils/styles'

/**
 * SearchBar — controlled search input.
 *
 * Shows a spinner while loading, and an × clear button when there is text.
 * Focuses the input automatically when the user presses Ctrl+K or /.
 */
export default function SearchBar({ value, onChange, loading = false, placeholder = 'Search transcripts…' }) {
  const inputRef = useRef(null)

  // Global keyboard shortcut: / or Ctrl+K focuses the search bar
  const handleGlobalKey = (e) => {
    if ((e.key === '/' || (e.key === 'k' && (e.ctrlKey || e.metaKey)))
        && document.activeElement !== inputRef.current) {
      e.preventDefault()
      inputRef.current?.focus()
    }
  }

  // Register/unregister on mount
  if (typeof window !== 'undefined') {
    window.addEventListener('keydown', handleGlobalKey)
    // Clean up immediately after registration (effect-less approach for simplicity)
    setTimeout(() => window.removeEventListener('keydown', handleGlobalKey), 0)
  }

  return (
    <div style={{ position: 'relative' }}>

      {/* Search icon */}
      <span style={{
        position: 'absolute', left: 12, top: '50%',
        transform: 'translateY(-50%)',
        color: colors.gray[400], fontSize: 16, pointerEvents: 'none',
        lineHeight: 1,
      }}>
        🔍
      </span>

      <input
        ref={inputRef}
        type="search"
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        aria-label="Search transcripts"
        style={{
          width: '100%',
          padding: '10px 40px 10px 38px',
          fontSize: 14,
          border: `1px solid ${value ? colors.primaryMid : colors.gray[200]}`,
          borderRadius: 10,
          outline: 'none',
          background: '#fff',
          color: colors.gray[800],
          fontFamily: 'inherit',
          transition: 'border-color 0.15s',
          boxSizing: 'border-box',
        }}
      />

      {/* Right side: spinner OR clear button */}
      <div style={{
        position: 'absolute', right: 10, top: '50%',
        transform: 'translateY(-50%)',
      }}>
        {loading ? (
          <svg width={16} height={16} viewBox="0 0 24 24" fill="none"
               style={{ animation: 'spin 0.8s linear infinite', display: 'block' }}>
            <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
            <circle cx="12" cy="12" r="10" stroke={colors.gray[300]} strokeWidth="2.5"/>
            <path d="M12 2a10 10 0 0 1 10 10" stroke={colors.primary}
                  strokeWidth="2.5" strokeLinecap="round"/>
          </svg>
        ) : value ? (
          <button
            onClick={() => onChange('')}
            aria-label="Clear search"
            style={{
              background: colors.gray[200], border: 'none',
              borderRadius: '50%', width: 18, height: 18,
              cursor: 'pointer', fontSize: 11,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: colors.gray[600], lineHeight: 1,
            }}
          >
            ×
          </button>
        ) : (
          <kbd style={{
            fontSize: 10, color: colors.gray[300],
            border: `1px solid ${colors.gray[200]}`,
            borderRadius: 3, padding: '1px 4px',
            fontFamily: 'monospace',
          }}>
            /
          </kbd>
        )}
      </div>
    </div>
  )
}
