/**
 * ErrorMessage — displays an API or validation error with an optional hint.
 */
export default function ErrorMessage({ message, hint, onRetry, style = {} }) {
  if (!message) return null

  return (
    <div
      role="alert"
      style={{
        background: '#fef2f2',
        border: '1px solid #fecaca',
        borderRadius: 8,
        padding: '12px 14px',
        fontSize: 14,
        color: '#991b1b',
        ...style,
      }}
    >
      <div style={{ fontWeight: 500, marginBottom: hint ? 4 : 0 }}>{message}</div>
      {hint && (
        <div style={{ color: '#b91c1c', fontSize: 12, marginTop: 2 }}>
          💡 {hint}
        </div>
      )}
      {onRetry && (
        <button
          onClick={onRetry}
          style={{
            marginTop: 8,
            fontSize: 13,
            color: '#dc2626',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: 0,
            textDecoration: 'underline',
          }}
        >
          Try again
        </button>
      )}
    </div>
  )
}
