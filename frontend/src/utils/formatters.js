/**
 * Formatting utilities shared across all components.
 */

// ── Date / time ───────────────────────────────────────────────────────────────

/** "Today, 9:14 AM"  /  "Yesterday"  /  "May 20"  /  "Jan 3, 2025" */
export function formatDate(isoString) {
  if (!isoString) return ''
  const date = new Date(isoString)
  const now  = new Date()

  const isToday = date.toDateString() === now.toDateString()
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  const isYesterday = date.toDateString() === yesterday.toDateString()

  if (isToday) {
    return 'Today, ' + date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
  }
  if (isYesterday) return 'Yesterday'

  const sameYear = date.getFullYear() === now.getFullYear()
  if (sameYear) {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
  }
  return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })
}

/** "0:00"  /  "1:23"  /  "1:02:45" */
export function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return '0:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) {
    return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return `${m}:${String(s).padStart(2, '0')}`
}

/** Recording timer: "0:00" counting up */
export function formatRecordingTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

// ── Numbers ───────────────────────────────────────────────────────────────────

/** 0.9872 → "98.7%" */
export function formatConfidence(score) {
  if (score == null) return '—'
  return (score * 100).toFixed(1) + '%'
}

/** 1234 → "1,234 words"  /  0 → "—" */
export function formatWordCount(count) {
  if (!count) return '—'
  return count.toLocaleString() + ' word' + (count === 1 ? '' : 's')
}

/** 52428800 → "50 MB"  /  1024 → "1 KB" */
export function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1).replace('.0', '') + ' ' + units[i]
}

/** 300 → "5 mins"  /  3661 → "1 hr 1 min" */
export function formatDurationLong(seconds) {
  if (!seconds) return '0 mins'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h > 0 && m > 0) return `${h} hr ${m} min`
  if (h > 0)           return `${h} hr`
  return `${m} min${m !== 1 ? 's' : ''}`
}

// ── Status ────────────────────────────────────────────────────────────────────

/** Maps status string to { label, color } for badge display */
export function getStatusBadge(status) {
  switch (status) {
    case 'done':       return { label: 'Done',       color: 'green'  }
    case 'processing': return { label: 'Processing', color: 'yellow' }
    case 'pending':    return { label: 'Pending',    color: 'gray'   }
    case 'failed':     return { label: 'Failed',     color: 'red'    }
    default:           return { label: status,       color: 'gray'   }
  }
}

// ── Language ──────────────────────────────────────────────────────────────────

const LANGUAGE_NAMES = {
  'en-US': 'English (US)',
  'en-GB': 'English (UK)',
  'hi-IN': 'Hindi',
  'es-ES': 'Spanish',
  'fr-FR': 'French',
  'de-DE': 'German',
  'ja-JP': 'Japanese',
  'zh-CN': 'Chinese',
  'ar-SA': 'Arabic',
  'pt-BR': 'Portuguese',
}

export function getLanguageName(code) {
  return LANGUAGE_NAMES[code] ?? code
}