import { useState } from 'react'
import { btnPrimary, btnSecondary, btnDanger, colors } from '../../utils/styles'
import Spinner from './Spinner'

/**
 * Button — primary / secondary / danger variants with loading state.
 *
 * Props:
 *   variant     'primary' | 'secondary' | 'danger'
 *   loading     bool — shows spinner, disables interaction
 *   fullWidth   bool
 *   type        'button' | 'submit' | 'reset'
 *   onClick, disabled, children, style
 */
export default function Button({
  variant   = 'primary',
  loading   = false,
  fullWidth = false,
  type      = 'button',
  onClick,
  disabled  = false,
  children,
  style     = {},
}) {
  const [hovered, setHovered] = useState(false)
  const isDisabled = disabled || loading

  const baseStyle = variant === 'primary'   ? btnPrimary
                  : variant === 'danger'    ? btnDanger
                  : btnSecondary

  const hoverStyle = !isDisabled && hovered ? (
    variant === 'primary' ? { background: colors.primaryDark }
    : variant === 'danger'  ? { background: colors.dangerLight }
    : { background: colors.gray[50] }
  ) : {}

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={isDisabled}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        ...baseStyle,
        ...hoverStyle,
        width:   fullWidth ? '100%' : undefined,
        opacity: isDisabled ? 0.65 : 1,
        cursor:  isDisabled ? 'not-allowed' : 'pointer',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        ...style,
      }}
    >
      {loading && <Spinner size="sm" label="" />}
      {children}
    </button>
  )
}
