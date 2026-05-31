import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider, useAuth } from '../context/AuthContext'
import authService from '../services/authService'

// Mock the whole authService module
vi.mock('../services/authService')

const wrapper = ({ children }) => (
  <MemoryRouter>
    <AuthProvider>{children}</AuthProvider>
  </MemoryRouter>
)

beforeEach(() => {
  localStorage.clear()
  vi.clearAllMocks()
  // Default: /me throws (no stored token on most tests)
  authService.me.mockRejectedValue(new Error('No token'))
})

// ── Initial state ─────────────────────────────────────────────────────────────
describe('AuthContext initial state', () => {
  it('starts with null user and token', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    // Wait for the useEffect to finish
    await act(async () => {})
    expect(result.current.user).toBeNull()
    expect(result.current.token).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })

  it('loading is false after mount check', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    expect(result.current.loading).toBe(false)
  })
})

// ── login ─────────────────────────────────────────────────────────────────────
describe('login()', () => {
  it('sets user and token on success', async () => {
    authService.login.mockResolvedValue({
      token: 'mock.jwt.token',
      id: 1, name: 'Arjun', email: 'arjun@test.com'
    })
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    await act(async () => {
      await result.current.login('arjun@test.com', 'TestPass1')
    })
    expect(result.current.user).toMatchObject({ id: 1, name: 'Arjun' })
    expect(result.current.token).toBe('mock.jwt.token')
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('persists token to localStorage', async () => {
    authService.login.mockResolvedValue({
      token: 'persisted.token', id: 1, name: 'A', email: 'a@a.com'
    })
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    await act(async () => {
      await result.current.login('a@a.com', 'pass')
    })
    expect(localStorage.getItem('token')).toBe('persisted.token')
    expect(JSON.parse(localStorage.getItem('user')).email).toBe('a@a.com')
  })

  it('throws when authService.login throws', async () => {
    const err = Object.assign(new Error('Bad creds'), { apiMessage: 'Email or password is incorrect' })
    authService.login.mockRejectedValue(err)
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    await expect(act(async () => {
      await result.current.login('x@x.com', 'wrong')
    })).rejects.toThrow()
    expect(result.current.isAuthenticated).toBe(false)
  })
})

// ── register ──────────────────────────────────────────────────────────────────
describe('register()', () => {
  it('sets user and token on success', async () => {
    authService.register.mockResolvedValue({
      token: 'new.token', id: 2, name: 'Priya', email: 'priya@test.com'
    })
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    await act(async () => {
      await result.current.register('Priya', 'priya@test.com', 'TestPass1')
    })
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user?.name).toBe('Priya')
  })
})

// ── logout ────────────────────────────────────────────────────────────────────
describe('logout()', () => {
  it('clears user, token, and localStorage', async () => {
    authService.login.mockResolvedValue({
      token: 't', id: 1, name: 'U', email: 'u@u.com'
    })
    authService.logout.mockResolvedValue({})

    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    await act(async () => { await result.current.login('u@u.com', 'p') })
    expect(result.current.isAuthenticated).toBe(true)

    await act(async () => { await result.current.logout() })
    expect(result.current.user).toBeNull()
    expect(result.current.token).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('still clears auth if server logout call fails', async () => {
    authService.login.mockResolvedValue({ token: 't', id:1, name:'U', email:'u@u.com' })
    authService.logout.mockRejectedValue(new Error('network'))
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    await act(async () => { await result.current.login('u@u.com', 'p') })
    await act(async () => { await result.current.logout() })
    expect(result.current.isAuthenticated).toBe(false)
  })
})

// ── updateUser ────────────────────────────────────────────────────────────────
describe('updateUser()', () => {
  it('patches user fields without full re-login', async () => {
    authService.login.mockResolvedValue({ token:'t', id:1, name:'Old', email:'u@u.com' })
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    await act(async () => { await result.current.login('u@u.com', 'p') })
    act(() => { result.current.updateUser({ name: 'New Name' }) })
    expect(result.current.user?.name).toBe('New Name')
  })
})

// ── Session restore ───────────────────────────────────────────────────────────
describe('session restore from localStorage', () => {
  it('restores session when valid token stored', async () => {
    localStorage.setItem('token', 'stored.token')
    localStorage.setItem('user', JSON.stringify({ id:1, name:'Stored', email:'s@s.com' }))
    authService.me.mockResolvedValue({ email: 's@s.com', authenticated: true })

    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {})
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user?.name).toBe('Stored')
  })

  it('clears auth when stored token is invalid', async () => {
    localStorage.setItem('token', 'expired.token')
    localStorage.setItem('user', JSON.stringify({ id:1, name:'Old', email:'o@o.com' }))
    authService.me.mockRejectedValue(new Error('401'))

    const { result } = renderHook(() => useAuth(), { wrapper })
    // Wait for the background validation to resolve
    await act(async () => { await new Promise(r => setTimeout(r, 50)) })
    expect(result.current.isAuthenticated).toBe(false)
  })
})
