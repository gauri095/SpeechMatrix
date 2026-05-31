/**
 * Client-side validators that mirror the Spring @ValidPassword rules
 * so errors show instantly without a round trip.
 */

export const validators = {

  required: (label = 'This field') => (value) =>
    !value?.trim() ? `${label} is required` : null,

  email: (value) => {
    if (!value?.trim()) return 'Email is required'
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return re.test(value) ? null : 'Invalid email format'
  },

  /**
   * Password strength — mirrors PasswordConstraintValidator.java:
   *  - 8+ characters
   *  - At least one uppercase letter
   *  - At least one lowercase letter
   *  - At least one digit
   */
  password: (value) => {
    if (!value) return 'Password is required'
    const missing = []
    if (value.length < 8)          missing.push('at least 8 characters')
    if (!/[A-Z]/.test(value))      missing.push('one uppercase letter')
    if (!/[a-z]/.test(value))      missing.push('one lowercase letter')
    if (!/\d/.test(value))         missing.push('one digit')
    return missing.length
      ? 'Password must contain: ' + missing.join(', ')
      : null
  },

  minLength: (min, label = 'Value') => (value) =>
    (value?.length ?? 0) < min
      ? `${label} must be at least ${min} characters`
      : null,

  maxLength: (max, label = 'Value') => (value) =>
    (value?.length ?? 0) > max
      ? `${label} must be under ${max} characters`
      : null,

  /** Validates BCP-47 language code e.g. en-US, hi-IN */
  languageCode: (value) =>
    value && !/^[a-z]{2}-[A-Z]{2}$/.test(value)
      ? 'Language must be a BCP-47 code like en-US or hi-IN'
      : null,

  /** Run multiple validators and return the first error */
  compose: (...fns) => (value) => {
    for (const fn of fns) {
      const err = fn(value)
      if (err) return err
    }
    return null
  },
}

/**
 * Validate an entire form object.
 * @param {object} values   form field values
 * @param {object} rules    { fieldName: validatorFn }
 * @returns {object} errors { fieldName: errorString } — empty if all valid
 */
export function validateForm(values, rules) {
  const errors = {}
  for (const [field, validate] of Object.entries(rules)) {
    const err = validate(values[field])
    if (err) errors[field] = err
  }
  return errors
}

/** Returns true if an errors object from validateForm has no entries */
export const isValid = (errors) => Object.keys(errors).length === 0