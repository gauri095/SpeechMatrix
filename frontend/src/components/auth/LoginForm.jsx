import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../Toast'
import FormField from '../FormField'
import Button from '../common/Button'
import ErrorMessage from '../ErrorMessage'
import { validators, validateForm, isValid } from '../../utils/validators'
import { colors } from '../../utils/styles'

/**
 * LoginForm — email + password form with:
 *  - Real-time validation after first blur
 *  - Server error displayed as a banner (not a field error)
 *  - "Remember where you were" redirect after successful login
 */
export default function LoginForm() {
  const navigate  = useNavigate()
  const location  = useLocation()
  const { login } = useAuth()
  const toast     = useToast()

  const redirectTo = location.state?.from?.pathname ?? '/dashboard'
  const sessionExpired = new URLSearchParams(location.search).get('reason') === 'session_expired'

  const [values,  setValues]  = useState({ email: '', password: '' })
  const [errors,  setErrors]  = useState({})
  const [touched, setTouched] = useState({})
  const [apiError, setApiError] = useState(null)
  const [apiHint,  setApiHint]  = useState(null)
  const [loading, setLoading]  = useState(false)

  // Validation rules — mirror the Spring constraints
  const rules = {
    email:    validators.email,
    password: validators.required('Password'),
  }

  const validate = (vals = values) => {
    const errs = validateForm(vals, rules)
    setErrors(errs)
    return isValid(errs)
  }

  const handleChange = (field) => (e) => {
    const updated = { ...values, [field]: e.target.value }
    setValues(updated)
    // Re-validate the changed field if it's been touched
    if (touched[field]) {
      const errs = validateForm(updated, rules)
      setErrors(prev => ({ ...prev, [field]: errs[field] }))
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
    // Touch all fields so errors show
    setTouched({ email: true, password: true })
    if (!validate()) return

    setLoading(true)
    setApiError(null)
    setApiHint(null)

    try {
      await login(values.email, values.password)
      toast.success('Welcome back!')
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setApiError(err.apiMessage ?? 'Login failed. Please try again.')
      setApiHint(err.apiHint ?? null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {/* Session expired banner */}
      {sessionExpired && !apiError && (
        <div style={{
          background: '#FEF9C3', border: '1px solid #FDE68A',
          borderRadius: 8, padding: '10px 14px',
          fontSize: 13, color: '#92400E', marginBottom: 20,
        }}>
          Your session has expired. Please log in again.
        </div>
      )}

      {/* Server error banner */}
      <ErrorMessage message={apiError} hint={apiHint} style={{ marginBottom: 20 }} />

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
        autoFocus
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
        placeholder="Your password"
        required
        disabled={loading}
      />

      <Button type="submit" fullWidth loading={loading} style={{ marginTop: 4 }}>
        {loading ? 'Signing in…' : 'Sign in'}
      </Button>

      <p style={{ marginTop: 20, textAlign: 'center', fontSize: 14, color: colors.gray[500] }}>
        Don't have an account?{' '}
        <Link to="/register" style={{ color: colors.primary, fontWeight: 500 }}>
          Sign up
        </Link>
      </p>
    </form>
  )
}
