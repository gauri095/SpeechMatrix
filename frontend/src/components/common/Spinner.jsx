/**
 * Spinner — inline or full-page loading indicator.
 * size: 'sm' | 'md' | 'lg'
 */
export default function Spinner({ size = 'md', label = 'Loading…', fullPage = false }) {
  const dim = { sm: 16, md: 24, lg: 40 }[size]
  const stroke = { sm: 2, md: 2.5, lg: 3 }[size]

  const svg = (
    <svg
      width={dim} height={dim}
      viewBox="0 0 24 24"
      fill="none"
      style={{ animation: 'spin 0.8s linear infinite' }}
      aria-hidden="true"
    >
      <style>{`@keyframes spin { to { transform: rotate(360deg) } }`}</style>
      <circle
        cx="12" cy="12" r="10"
        stroke="currentColor" strokeWidth={stroke}
        strokeOpacity="0.25"
      />
      <path
        d="M12 2a10 10 0 0 1 10 10"
        stroke="currentColor" strokeWidth={stroke}
        strokeLinecap="round"
      />
    </svg>
  )

  if (fullPage) {
    return (
      <div style={{
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
        minHeight: '60vh', gap: 12, color: '#6b7280',
      }}>
        {svg}
        {label && <span style={{ fontSize: 14 }}>{label}</span>}
      </div>
    )
  }

  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, color: '#6b7280' }}>
      {svg}
      {label && <span style={{ fontSize: 13 }}>{label}</span>}
    </span>
  )
}
