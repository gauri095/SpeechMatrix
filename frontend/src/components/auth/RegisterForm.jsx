import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../Toast'
import FormField from '../FormField'
import Button from '../Button'
import ErrorMessage from '../ErrorMessage'
import { validators, validateForm, isValid } from '../../utils/validators'
import { colors } from '../../utils/styles'

/**
 * RegisterForm — name + email + password + confirm with:
 *  - Live password strength meter (4 requirements)
 *  - Confirm password must match
 *  - Server-side duplicate email shown inline on the email field
 */
export default function RegisterForm() {
  const navigate    = useNavigate()
  const { register } = useAuth()
  const toast       = useToast()

  const [values,  setValues]  = useState({ name: '', email: '', password: '', confirm: '' })
  const [errors,  setErrors]  = useState({})
  const [touched, setTouched] = useState({})
  const [apiError, setApiError] = useState(null)
  const [loading, setLoading]  = useState(false)

  const rules = {
    name:     validators.compose(
                validators.required('Name'),
                validators.minLength(2, 'Name'),
                validators.maxLength(100, 'Name')),
    email:    validators.email,
    password: validators.password,
    confirm:  (v) => v !== values.password ? 'Passwords do not match' : null,
  }

  const validate = (vals = values) => {
    const errs = validateForm(vals, rules)
    setErrors(errs)
    return isValid(errs)
  }

  const handleChange = (field) => (e) => {
    const updated = { ...values, [field]: e.target.value }
    setValues(updated)
    if (touched[field]) {
      const errs = validateForm(updated, { ...rules,
        // Re-check confirm whenever password changes
        ...(field === 'password' ? {
          confirm: (v) => v !== updated.password ? 'Passwords do not match' : null
        } : {})
      })
      setErrors(prev => ({ ...prev, [field]: errs[field],
        ...(field === 'password' ? { confirm: errs.confirm } : {}) }))
    }
    setApiError(null)
  }

  const handleBlur = (field) => () => {
    setTouched(prev => ({ ...prev, [field]: true }))
    const errs = validateForm(values, rules)
    setErrors(prev => ({ ...prev, [field]: errs[field] }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setTouched({ name: true, email: true, password: true, confirm: true })
    if (!validate()) return

    setLoading(true)
    setApiError(null)

    try {
      await register(values.name.trim(), values.email, values.password)
      toast.success('Account created! Welcome to VoxScript.')
      navigate('/dashboard', { replace: true })
    } catch (err) {
      // Duplicate email → show on the email field specifically
      if (err.apiMessage?.includes('already registered')) {
        setErrors(prev => ({ ...prev, email: 'This email is already registered' }))
        setTouched(prev => ({ ...prev, email: true }))
      } else {
        setApiError(err.apiMessage ?? 'Registration failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <ErrorMessage message={apiError} style={{ marginBottom: 20 }} />

      <FormField
        id="name"
        label="Full name"
        type="text"
        value={values.name}
        onChange={handleChange('name')}
        onBlur={handleBlur('name')}
        error={touched.name ? errors.name : undefined}
        placeholder="Your name"
        required
        autoFocus
        disabled={loading}
      />

      <FormField
        id="email"
        label="Email address"
        type="email"
        value={values.email}
        onChange={handleChange('email')}
        onBlur={handleBlur('email')}
        error={touched.email ? errors.email : undefined}
        placeholder="you@example.com"
        required
        disabled={loading}
      />

      <FormField
        id="password"
        label="Password"
        type="password"
        value={values.password}
        onChange={handleChange('password')}
        onBlur={handleBlur('password')}
        error={touched.password ? errors.password : undefined}
        hint="8+ characters, uppercase, lowercase, and a digit"
        required
        disabled={loading}
      />

      {/* Live password strength indicator */}
      {values.password.length > 0 && (
        <PasswordStrength password={values.password} />
      )}

      <FormField
        id="confirm"
        label="Confirm password"
        type="password"
        value={values.confirm}
        onChange={handleChange('confirm')}
        onBlur={handleBlur('confirm')}
        error={touched.confirm ? errors.confirm : undefined}
        placeholder="Repeat your password"
        required
        disabled={loading}
      />

      <Button type="submit" fullWidth loading={loading} style={{ marginTop: 4 }}>
        {loading ? 'Creating account…' : 'Create account'}
      </Button>

      <p style={{ marginTop: 20, textAlign: 'center', fontSize: 14, color: colors.gray[500] }}>
        Already have an account?{' '}
        <Link to="/login" style={{ color: colors.primary, fontWeight: 500 }}>
          Sign in
        </Link>
      </p>
    </form>
  )
}

// ── Password strength meter ───────────────────────────────────────────────────

function PasswordStrength({ password }) {
  const checks = [
    { label: '8+ characters',    pass: password.length >= 8 },
    { label: 'Uppercase letter', pass: /[A-Z]/.test(password) },
    { label: 'Lowercase letter', pass: /[a-z]/.test(password) },
    { label: 'A digit',          pass: /\d/.test(password)    },
  ]

  const passCount = checks.filter(c => c.pass).length
  const strength  = ['', 'Weak', 'Fair', 'Good', 'Strong'][passCount]
  const barColor  = ['', colors.danger, '#F59E0B', '#3B82F6', colors.primary][passCount]

  return (
    <div style={{ marginTop: -10, marginBottom: 18 }}>
      {/* Bar */}
      <div style={{
        display: 'flex', gap: 3, marginBottom: 6,
        height: 4, borderRadius: 2, overflow: 'hidden',
      }}>
        {[0,1,2,3].map(i => (
          <div
            key={i}
            style={{
              flex: 1, borderRadius: 2,
              background: i < passCount ? barColor : colors.gray[200],
              transition: 'background 0.2s',
            }}
          />
        ))}
      </div>

      {/* Checklist */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '2px 14px' }}>
        {checks.map(c => (
          <span
            key={c.label}
            style={{ fontSize: 11, color: c.pass ? colors.primary : colors.gray[400] }}
          >
            {c.pass ? '✓' : '○'} {c.label}
          </span>
        ))}
        {passCount > 0 && (
          <span style={{
            fontSize: 11, fontWeight: 600, color: barColor, marginLeft: 'auto',
          }}>
            {strength}
          </span>
        )}
      </div>
    </div>
  )
}
