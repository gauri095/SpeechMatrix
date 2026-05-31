import LoginForm from '../components/auth/LoginForm'
import { colors, authPage, authCard } from '../utils/styles'

export default function LoginPage() {
  return (
    <div style={authPage}>
      <div style={authCard}>

        {/* Branding */}
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <div style={{
            width: 48, height: 48, borderRadius: 12,
            background: colors.primary,
            display: 'inline-flex', alignItems: 'center',
            justifyContent: 'center', fontSize: 22,
            marginBottom: 12,
          }}>
            🎙
          </div>
          <h1 style={{ fontSize: 22, fontWeight: 700, color: colors.gray[900],
                        letterSpacing: '-0.02em', margin: '0 0 4px' }}>
            Welcome back
          </h1>
          <p style={{ fontSize: 14, color: colors.gray[500], margin: 0 }}>
            Sign in to VoxScript
          </p>
        </div>

        <LoginForm />
      </div>
    </div>
  )
}