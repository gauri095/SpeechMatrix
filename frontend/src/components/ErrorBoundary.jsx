import { Component } from 'react'
import { colors } from '../../utils/styles'

/**
 * ErrorBoundary — catches errors in its child component tree.
 *
 * React error boundaries must be class components (hooks cannot catch
 * render errors). This one provides:
 *  - A friendly fallback UI with error details (dev) or generic message (prod)
 *  - A "Try again" button that resets the boundary
 *  - An optional onError callback for logging to Sentry etc.
 *
 * Usage:
 *   <ErrorBoundary>
 *     <SomeComponent />
 *   </ErrorBoundary>
 *
 *   <ErrorBoundary fallback={<p>Custom fallback</p>}>
 *     <SomeComponent />
 *   </ErrorBoundary>
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null, errorInfo: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    this.setState({ errorInfo })
    // Call optional onError prop (e.g. Sentry.captureException)
    this.props.onError?.(error, errorInfo)
    console.error('[ErrorBoundary]', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null })
  }

  render() {
    if (!this.state.hasError) return this.props.children

    // If a custom fallback is provided, use it
    if (this.props.fallback) return this.props.fallback

    const isDev = import.meta.env?.DEV ?? false

    return (
      <div
        role="alert"
        aria-label="Application error"
        style={{
          padding: '40px 24px',
          textAlign: 'center',
          maxWidth: 520,
          margin: '40px auto',
          background: '#fff',
          border: `1px solid ${colors.gray[200]}`,
          borderRadius: 16,
        }}
      >
        {/* Icon */}
        <div style={{ fontSize: 48, marginBottom: 16 }}>⚠️</div>

        <h2 style={{
          fontSize: 18, fontWeight: 700,
          color: colors.gray[900], margin: '0 0 8px',
        }}>
          Something went wrong
        </h2>

        <p style={{
          fontSize: 14, color: colors.gray[500],
          margin: '0 0 24px', lineHeight: 1.6,
        }}>
          {this.props.message ??
            'An unexpected error occurred. The error has been logged.'}
        </p>

        {/* Dev-only: show error details */}
        {isDev && this.state.error && (
          <details style={{
            textAlign: 'left',
            background: colors.gray[50],
            border: `1px solid ${colors.gray[200]}`,
            borderRadius: 8, padding: 12,
            marginBottom: 20, fontSize: 12,
          }}>
            <summary style={{ cursor: 'pointer', fontWeight: 600, color: colors.danger }}>
              Error details (dev only)
            </summary>
            <pre style={{
              marginTop: 8, overflow: 'auto',
              color: colors.danger, fontFamily: 'monospace',
              fontSize: 11, whiteSpace: 'pre-wrap',
            }}>
              {this.state.error.toString()}
              {this.state.errorInfo?.componentStack}
            </pre>
          </details>
        )}

        <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
          <button
            onClick={this.handleReset}
            style={{
              padding: '9px 20px', fontSize: 14, fontWeight: 600,
              background: colors.primary, color: '#fff',
              border: 'none', borderRadius: 8, cursor: 'pointer',
              fontFamily: 'inherit',
            }}
          >
            Try again
          </button>
          <button
            onClick={() => window.location.reload()}
            style={{
              padding: '9px 20px', fontSize: 14, fontWeight: 500,
              background: '#fff', color: colors.gray[600],
              border: `1px solid ${colors.gray[200]}`,
              borderRadius: 8, cursor: 'pointer',
              fontFamily: 'inherit',
            }}
          >
            Reload page
          </button>
        </div>
      </div>
    )
  }
}