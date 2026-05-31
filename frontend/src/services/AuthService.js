import api from './api'

const AUTH = '/api/auth'

const authService = {

  /** Register new account. Returns { token, id, name, email }. */
  register: (name, email, password) =>
    api.post(`${AUTH}/register`, { name, email, password })
       .then(r => r.data),

  /** Login. Returns { token, id, name, email }. */
  login: (email, password) =>
    api.post(`${AUTH}/login`, { email, password })
       .then(r => r.data),

  /** Logout — blacklists the JWT on the server. */
  logout: () =>
    api.post(`${AUTH}/logout`).then(r => r.data),

  /** Get full profile with usage statistics. */
  getProfile: () =>
    api.get(`${AUTH}/profile`).then(r => r.data),

  /** Update display name. */
  updateProfile: (name) =>
    api.put(`${AUTH}/profile`, { name }).then(r => r.data),

  /** Change password. */
  changePassword: (currentPassword, newPassword) =>
    api.post(`${AUTH}/change-password`, { currentPassword, newPassword })
       .then(r => r.data),

  /** Quick check — is the current token still valid? */
  me: () =>
    api.get(`${AUTH}/me`).then(r => r.data),
}

export default authService