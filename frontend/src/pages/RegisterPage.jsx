import RegisterForm from '../components/auth/RegisterForm'
import { colors, authPage, authCard } from '../utils/styles'

export default function RegisterPage() {
  return (
    <div style={authPage}>
      <div style={authCard}>

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
            Create an account
          </h1>
          <p style={{ fontSize: 14, color: colors.gray[500], margin: 0 }}>
            Start transcribing in seconds
          </p>
        </div>

        <RegisterForm />
      </div>
    </div>
  )
}