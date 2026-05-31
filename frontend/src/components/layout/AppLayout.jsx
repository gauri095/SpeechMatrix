import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../common/Toast'
import { useIsTablet } from '../../hooks/useMediaQuery'
import { useKeyboard } from '../../hooks/useKeyboard'
import { colors } from '../../utils/styles'

const NAV_ITEMS = [
  { to: '/dashboard', icon: '⊞', label: 'Dashboard'  },
  { to: '/upload',    icon: '↑',  label: 'Upload'     },
  { to: '/record',    icon: '●',  label: 'Record'     },
  { to: '/history',   icon: '☰',  label: 'History'    },
]

export default function AppLayout({ children }) {
  const { user, logout }  = useAuth()
  const { pathname }      = useLocation()
  const navigate          = useNavigate()
  const toast             = useToast()
  const isTablet          = useIsTablet()
  const [menuOpen, setMenuOpen] = useState(false)

  // Close mobile menu on Escape
  useKeyboard('Escape', () => setMenuOpen(false), { enabled: menuOpen })

  const handleLogout = async () => {
    setMenuOpen(false)
    await logout()
    toast.info('Signed out.')
    navigate('/login', { replace: true })
  }

  const isActive = (to) =>
    pathname === to || (to !== '/dashboard' && pathname.startsWith(to))

  return (
    <div style={{ minHeight: '100vh', background: colors.gray[50] }}>

      {/* ── Skip navigation ──────────────────────────────────────────────── */}
      <a href="#main-content" className="skip-nav">
        Skip to main content
      </a>

      {/* ── Top nav ──────────────────────────────────────────────────────── */}
      <nav
        aria-label="Main navigation"
        style={{
          position: 'sticky', top: 0, zIndex: 100,
          background: '#fff',
          borderBottom: `1px solid ${colors.gray[200]}`,
          padding: `0 ${isTablet ? '16px' : '24px'}`,
          height: 56,
          display: 'flex', alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        {/* Logo */}
        <Link
          to="/dashboard"
          aria-label="VoxScript home"
          style={{
            fontWeight: 700, fontSize: 17,
            color: colors.gray[900], letterSpacing: '-0.02em',
            display: 'flex', alignItems: 'center', gap: 8,
            textDecoration: 'none',
            minHeight: 44, minWidth: 44,
          }}
        >
          <span style={{
            width: 28, height: 28, borderRadius: 7,
            background: colors.primary,
            display: 'inline-flex', alignItems: 'center',
            justifyContent: 'center', fontSize: 14, flexShrink: 0,
          }} aria-hidden="true">🎙</span>
          {!isTablet && (
            <>Vox<span style={{ color: colors.primary }}>Script</span></>
          )}
        </Link>

        {/* Desktop nav links */}
        {!isTablet && (
          <div style={{ display: 'flex', gap: 4 }} role="list">
            {NAV_ITEMS.map(({ to, icon, label }) => (
              <Link
                key={to}
                to={to}
                role="listitem"
                aria-current={isActive(to) ? 'page' : undefined}
                style={{
                  display: 'flex', alignItems: 'center', gap: 6,
                  padding: '5px 12px', minHeight: 44,
                  borderRadius: 7, fontSize: 13, fontWeight: 500,
                  textDecoration: 'none',
                  color:      isActive(to) ? colors.primary : colors.gray[600],
                  background: isActive(to) ? colors.primaryLight : 'transparent',
                  transition: 'background 0.15s',
                }}
              >
                <span aria-hidden="true">{icon}</span>
                <span>{label}</span>
              </Link>
            ))}
          </div>
        )}

        {/* Right side: user + actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {!isTablet && (
            <span style={{ fontSize: 13, color: colors.gray[500] }}
                  aria-label={`Signed in as ${user?.name}`}>
              {user?.name}
            </span>
          )}

          {!isTablet && (
            <button
              onClick={handleLogout}
              aria-label="Sign out"
              style={{
                fontSize: 13, padding: '5px 12px', minHeight: 44,
                background: 'none',
                border: `1px solid ${colors.gray[200]}`,
                borderRadius: 7, color: colors.gray[600],
                cursor: 'pointer', fontFamily: 'inherit',
              }}
            >
              Sign out
            </button>
          )}

          {/* Mobile hamburger */}
          {isTablet && (
            <button
              onClick={() => setMenuOpen(v => !v)}
              aria-label={menuOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={menuOpen}
              aria-controls="mobile-menu"
              style={{
                background: 'none', border: 'none',
                fontSize: 22, cursor: 'pointer',
                color: colors.gray[700],
                padding: '0 4px', minHeight: 44, minWidth: 44,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}
            >
              {menuOpen ? '✕' : '☰'}
            </button>
          )}
        </div>
      </nav>

      {/* ── Mobile menu drawer ────────────────────────────────────────────── */}
      {isTablet && menuOpen && (
        <>
          {/* Backdrop */}
          <div
            onClick={() => setMenuOpen(false)}
            aria-hidden="true"
            style={{
              position: 'fixed', inset: 0, zIndex: 99,
              background: 'rgba(0,0,0,0.3)',
            }}
          />
          {/* Drawer */}
          <nav
            id="mobile-menu"
            aria-label="Mobile navigation"
            style={{
              position: 'fixed', top: 56, left: 0, right: 0,
              zIndex: 100, background: '#fff',
              borderBottom: `1px solid ${colors.gray[200]}`,
              padding: '12px 16px 16px',
            }}
          >
            {NAV_ITEMS.map(({ to, icon, label }) => (
              <Link
                key={to}
                to={to}
                onClick={() => setMenuOpen(false)}
                aria-current={isActive(to) ? 'page' : undefined}
                style={{
                  display: 'flex', alignItems: 'center', gap: 12,
                  padding: '12px 16px', borderRadius: 10,
                  fontSize: 15, fontWeight: 500,
                  textDecoration: 'none',
                  color:      isActive(to) ? colors.primary : colors.gray[700],
                  background: isActive(to) ? colors.primaryLight : 'transparent',
                  marginBottom: 4,
                }}
              >
                <span aria-hidden="true" style={{ fontSize: 18 }}>{icon}</span>
                {label}
              </Link>
            ))}

            <div style={{
              marginTop: 12,
              paddingTop: 12,
              borderTop: `1px solid ${colors.gray[100]}`,
              display: 'flex', justifyContent: 'space-between',
              alignItems: 'center',
            }}>
              <span style={{ fontSize: 13, color: colors.gray[500] }}>
                {user?.name}
              </span>
              <button
                onClick={handleLogout}
                style={{
                  fontSize: 13, padding: '6px 14px',
                  background: 'none',
                  border: `1px solid ${colors.gray[200]}`,
                  borderRadius: 7, color: colors.gray[600],
                  cursor: 'pointer', fontFamily: 'inherit',
                }}
              >
                Sign out
              </button>
            </div>
          </nav>
        </>
      )}

      {/* ── Page content ──────────────────────────────────────────────────── */}
      <main
        id="main-content"
        tabIndex={-1}
        style={{
          maxWidth: 900,
          margin: '0 auto',
          padding: isTablet ? '24px 16px' : '32px 24px',
        }}
      >
        {children}
      </main>
    </div>
  )
}