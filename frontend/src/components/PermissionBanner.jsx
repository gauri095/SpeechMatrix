import { colors } from '../../utils/styles'

/**
 * PermissionBanner — shown when mic permission is blocked or unknown.
 *
 * hasPermission:
 *   null  → never asked yet (don't show anything)
 *   false → denied or error (show recovery steps)
 */
export default function PermissionBanner({ hasPermission, error }) {
  // Don't show anything until we've tried to get permission
  if (hasPermission !== false && !error) return null

  return (
    <div
      role="alert"
      style={{
        padding: '14px 16px',
        background: '#FFF7ED',
        border: '1px solid #FED7AA',
        borderRadius: 10,
        fontSize: 13,
      }}
    >
      <p style={{ fontWeight: 600, color: '#9A3412', margin: '0 0 6px' }}>
        🎙 Microphone access required
      </p>

      {error && (
        <p style={{ color: '#C2410C', margin: '0 0 10px', lineHeight: 1.5 }}>
          {error}
        </p>
      )}

      <p style={{ color: '#92400E', margin: '0 0 8px', fontWeight: 500 }}>
        To fix this:
      </p>
      <ol style={{ color: '#92400E', margin: 0, paddingLeft: 20, lineHeight: 1.8 }}>
        <li>Click the 🔒 lock icon in your browser's address bar</li>
        <li>Find <strong>Microphone</strong> and set it to <strong>Allow</strong></li>
        <li>Refresh the page and try again</li>
      </ol>

      <p style={{ margin: '10px 0 0', fontSize: 12, color: colors.gray[400] }}>
        Your audio is never stored permanently without your consent.
      </p>
    </div>
  )
}
