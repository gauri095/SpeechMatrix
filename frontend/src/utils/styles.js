/**
 * Shared style constants — avoids duplicating inline styles across components.
 * All values align with the CSS variables defined in index.css.
 */

// ── Colours ───────────────────────────────────────────────────────────────────
export const colors = {
  primary:      '#1D9E75',
  primaryDark:  '#0F6E56',
  primaryLight: '#E1F5EE',
  primaryMid:   '#9FE1CB',
  danger:       '#DC2626',
  dangerLight:  '#FEF2F2',
  dangerBorder: '#FECACA',
  warning:      '#D97706',
  success:      '#16A34A',
  gray: {
    50:  '#F9FAFB', 100: '#F3F4F6', 200: '#E5E7EB',
    300: '#D1D5DB', 400: '#9CA3AF', 500: '#6B7280',
    600: '#4B5563', 700: '#374151', 800: '#1F2937', 900: '#111827',
  }
}

// ── Input ─────────────────────────────────────────────────────────────────────
export const inputBase = {
  width: '100%',
  padding: '10px 12px',
  fontSize: 14,
  border: `1px solid ${colors.gray[300]}`,
  borderRadius: 8,
  outline: 'none',
  background: '#fff',
  color: colors.gray[800],
  transition: 'border-color 0.15s',
  fontFamily: 'inherit',
}

export const inputFocused = {
  borderColor: colors.primary,
  boxShadow: `0 0 0 3px ${colors.primaryLight}`,
}

export const inputError = {
  borderColor: colors.danger,
  boxShadow: `0 0 0 3px ${colors.dangerLight}`,
}

// ── Buttons ───────────────────────────────────────────────────────────────────
export const btnPrimary = {
  width: '100%',
  padding: '11px 16px',
  fontSize: 14,
  fontWeight: 600,
  color: '#fff',
  background: colors.primary,
  border: 'none',
  borderRadius: 8,
  cursor: 'pointer',
  transition: 'background 0.15s, transform 0.1s',
  fontFamily: 'inherit',
}

export const btnSecondary = {
  padding: '9px 16px',
  fontSize: 14,
  fontWeight: 500,
  color: colors.gray[700],
  background: '#fff',
  border: `1px solid ${colors.gray[300]}`,
  borderRadius: 8,
  cursor: 'pointer',
  transition: 'background 0.15s',
  fontFamily: 'inherit',
}

export const btnDanger = {
  ...btnSecondary,
  color: colors.danger,
  borderColor: colors.dangerBorder,
}

// ── Card ──────────────────────────────────────────────────────────────────────
export const card = {
  background: '#fff',
  border: `1px solid ${colors.gray[200]}`,
  borderRadius: 12,
  padding: '20px',
}

// ── Label ─────────────────────────────────────────────────────────────────────
export const label = {
  display: 'block',
  fontSize: 13,
  fontWeight: 500,
  color: colors.gray[700],
  marginBottom: 5,
}

// ── Field error ───────────────────────────────────────────────────────────────
export const fieldError = {
  fontSize: 12,
  color: colors.danger,
  marginTop: 4,
}

// ── Auth page container ───────────────────────────────────────────────────────
export const authPage = {
  minHeight: '100vh',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: colors.gray[50],
  padding: '24px 16px',
}

export const authCard = {
  width: '100%',
  maxWidth: 420,
  background: '#fff',
  border: `1px solid ${colors.gray[200]}`,
  borderRadius: 16,
  padding: '36px 32px',
  boxShadow: '0 4px 24px rgb(0 0 0 / 0.06)',
}