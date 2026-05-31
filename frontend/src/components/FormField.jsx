import { useState } from 'react'
import { inputBase, inputFocused, inputError, label, fieldError, colors } from '../../utils/styles'

/**
 * FormField — labelled input with inline error and password toggle.
 *
 * Props:
 *   id, label, type, value, onChange, onBlur
 *   error      string — shown below the input when set
 *   hint       string — shown below as grey helper text
 *   required   bool
 *   autoFocus  bool
 *   disabled   bool
 *   placeholder string
 */
export default function FormField({
  id,
  label: labelText,
  type = 'text',
  value,
  onChange,
  onBlur,
  error,
  hint,
  required = false,
  autoFocus = false,
  disabled  = false,
  placeholder,
}) {
  const [focused,      setFocused]      = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const isPassword = type === 'password'
  const inputType  = isPassword && showPassword ? 'text' : type

  const borderStyle = error
    ? inputError
    : focused ? inputFocused : {}

  return (
    <div style={{ marginBottom: 18 }}>
      {/* Label */}
      <label htmlFor={id} style={label}>
        {labelText}
        {required && (
          <span aria-hidden="true" style={{ color: colors.danger, marginLeft: 2 }}>*</span>
        )}
      </label>

      {/* Input wrapper — relative so the eye icon can be positioned */}
      <div style={{ position: 'relative' }}>
        <input
          id={id}
          type={inputType}
          value={value}
          onChange={onChange}
          onBlur={(e) => { setFocused(false); onBlur?.(e) }}
          onFocus={() => setFocused(true)}
          placeholder={placeholder}
          required={required}
          autoFocus={autoFocus}
          disabled={disabled}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
          style={{
            ...inputBase,
            ...borderStyle,
            paddingRight: isPassword ? 40 : undefined,
            opacity: disabled ? 0.6 : 1,
            cursor:  disabled ? 'not-allowed' : undefined,
          }}
        />

        {/* Show / hide password toggle */}
        {isPassword && (
          <button
            type="button"
            onClick={() => setShowPassword(v => !v)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            style={{
              position: 'absolute', right: 10, top: '50%',
              transform: 'translateY(-50%)',
              background: 'none', border: 'none',
              cursor: 'pointer', padding: 4,
              color: colors.gray[400], fontSize: 16, lineHeight: 1,
            }}
          >
            {showPassword ? '🙈' : '👁'}
          </button>
        )}
      </div>

      {/* Field-level error */}
      {error && (
        <p id={`${id}-error`} role="alert" style={fieldError}>
          {error}
        </p>
      )}

      {/* Helper hint */}
      {hint && !error && (
        <p id={`${id}-hint`} style={{ ...fieldError, color: colors.gray[500] }}>
          {hint}
        </p>
      )}
    </div>
  )
}
