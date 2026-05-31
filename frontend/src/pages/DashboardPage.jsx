import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import AppLayout from '../components/layout/AppLayout'
import Spinner   from '../components/common/Spinner'
import { useAuth } from '../context/AuthContext'
import { useApi }  from '../hooks/useApi'
import speechService from '../services/speechService'
import { colors, card } from '../utils/styles'
import { formatWordCount, formatDurationLong } from '../utils/formatters'

export default function DashboardPage() {
  const { user } = useAuth()
  const { data: stats, loading, execute } = useApi(speechService.getStats)

  useEffect(() => { execute() }, [])

  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'

  return (
    <AppLayout>
      <div style={{ maxWidth: 700 }}>

        {/* Greeting */}
        <div style={{ marginBottom: 28 }}>
          <h1 style={{ fontSize: 24, fontWeight: 700, color: colors.gray[900],
                        letterSpacing: '-0.02em', margin: '0 0 4px' }}>
            {greeting}, {user?.name?.split(' ')[0]} 👋
          </h1>
          <p style={{ fontSize: 14, color: colors.gray[500], margin: 0 }}>
            What would you like to transcribe today?
          </p>
        </div>

        {/* Quick actions */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 28 }}>
          {[
            { to: '/upload', icon: '📤', title: 'Upload file',      sub: 'MP3, WAV, M4A, FLAC…' },
            { to: '/record', icon: '🎙', title: 'Record audio',     sub: 'Use your microphone'  },
            { to: '/history',icon: '📋', title: 'View history',     sub: 'All transcriptions'   },
          ].map(({ to, icon, title, sub }) => (
            <Link key={to} to={to} style={{
              ...card,
              display: 'flex', alignItems: 'center', gap: 14,
              textDecoration: 'none', color: 'inherit',
              transition: 'border-color 0.15s, box-shadow 0.15s',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.borderColor = colors.primaryMid
              e.currentTarget.style.boxShadow  = `0 2px 8px ${colors.primaryLight}`
            }}
            onMouseLeave={e => {
              e.currentTarget.style.borderColor = colors.gray[200]
              e.currentTarget.style.boxShadow  = 'none'
            }}>
              <span style={{ fontSize: 28 }}>{icon}</span>
              <div>
                <p style={{ fontWeight: 600, fontSize: 14, color: colors.gray[800], margin: 0 }}>{title}</p>
                <p style={{ fontSize: 12, color: colors.gray[400], margin: '2px 0 0' }}>{sub}</p>
              </div>
            </Link>
          ))}
        </div>

        {/* Stats */}
        <h2 style={{ fontSize: 15, fontWeight: 600, color: colors.gray[700], margin: '0 0 12px' }}>
          Your usage
        </h2>

        {loading && <Spinner label="Loading stats…" />}

        {stats && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(140px,1fr))', gap: 10 }}>
            {[
              { label: 'Transcriptions', value: stats.totalTranscriptions, icon: '📄' },
              { label: 'Total words',    value: formatWordCount(stats.totalWords), icon: '📝' },
              { label: 'Audio time',     value: formatDurationLong(stats.totalDurationSeconds), icon: '⏱' },
              { label: 'Completed',      value: stats.doneCount, icon: '✅' },
            ].map(({ label, value, icon }) => (
              <div key={label} style={{ ...card, background: colors.gray[50] }}>
                <p style={{ fontSize: 11, color: colors.gray[400], margin: '0 0 4px' }}>{icon} {label}</p>
                <p style={{ fontSize: 20, fontWeight: 700, color: colors.gray[800], margin: 0 }}>{value}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </AppLayout>
  )
}