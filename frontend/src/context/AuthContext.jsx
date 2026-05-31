import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import authService from '../services/authService'

/**
 * AuthContext — global authentication state.
 *
 * Provides:
 *   user        { id, name, email }  or  null
 *   token       JWT string           or  null
 *   loading     true while validating stored token on app start
 *   login()     logs in and persists token
 *   register()  registers and auto-logs-in
 *   logout()    clears state + calls server blacklist
 *   updateUser() patches user fields after profile edit
 *
 * Token storage: localStorage.
 * On mount the context calls /api/auth/me to verify the stored token
 * is still valid — if not, it clears auth silently.
 */

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null)
  const [token,   setToken]   = useState(null)
  const [loading, setLoading] = useState(true)   // checking stored token

  // ── On mount: restore session from localStorage ────────────────────────────
  useEffect(() => {
    const stored = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')

    if (stored && storedUser) {
      try {
        // Optimistically restore from storage
        setToken(stored)
        setUser(JSON.parse(storedUser))

        // Then verify with the server in background
        authService.me()
          .catch(() => {
            // Token expired or invalid — clear silently
            clearAuth()
          })
      } catch {
        clearAuth()
      }
    }
    setLoading(false)
  }, [])

  // ── Login ──────────────────────────────────────────────────────────────────
  const login = useCallback(async (email, password) => {
    const data = await authService.login(email, password)
    persistAuth(data)
    return data
  }, [])

  // ── Register ───────────────────────────────────────────────────────────────
  const register = useCallback(async (name, email, password) => {
    const data = await authService.register(name, email, password)
    persistAuth(data)
    return data
  }, [])

  // ── Logout ─────────────────────────────────────────────────────────────────
  const logout = useCallback(async () => {
    try {
      await authService.logout()   // blacklist token on server
    } catch {
      // Continue even if server call fails
    }
    clearAuth()
  }, [])

  // ── Update user fields (e.g. after profile name change) ───────────────────
  const updateUser = useCallback((patch) => {
    setUser(prev => {
      const updated = { ...prev, ...patch }
      localStorage.setItem('user', JSON.stringify(updated))
      return updated
    })
  }, [])

  // ── Helpers ────────────────────────────────────────────────────────────────
  const persistAuth = (data) => {
    const userObj = { id: data.id, name: data.name, email: data.email }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user',  JSON.stringify(userObj))
    setToken(data.token)
    setUser(userObj)
  }

  const clearAuth = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

  const value = {
    user,
    token,
    loading,
    isAuthenticated: !!user,
    login,
    register,
    logout,
    updateUser,
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

/** Hook — use this inside any component that needs auth state */
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
