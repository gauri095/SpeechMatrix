import { describe, it, expect } from 'vitest'
import { validators, validateForm, isValid } from '../utils/validators'

// ── email ─────────────────────────────────────────────────────────────────────
describe('validators.email', () => {
  it('accepts valid email',           () => expect(validators.email('a@b.com')).toBeNull())
  it('rejects missing @',             () => expect(validators.email('notanemail')).toBeTruthy())
  it('rejects empty string',          () => expect(validators.email('')).toBeTruthy())
  it('rejects null',                  () => expect(validators.email(null)).toBeTruthy())
  it('rejects missing domain',        () => expect(validators.email('a@')).toBeTruthy())
})

// ── password ──────────────────────────────────────────────────────────────────
describe('validators.password', () => {
  it('accepts strong password',       () => expect(validators.password('TestPass1')).toBeNull())
  it('accepts with special char',     () => expect(validators.password('Test@Pass1')).toBeNull())
  it('rejects too short',             () => expect(validators.password('Ab1')).toMatch(/8/))
  it('rejects no uppercase',          () => expect(validators.password('testpass1')).toMatch(/uppercase/))
  it('rejects no lowercase',          () => expect(validators.password('TESTPASS1')).toMatch(/lowercase/))
  it('rejects no digit',              () => expect(validators.password('TestPass!')).toMatch(/digit/))
  it('rejects null',                  () => expect(validators.password(null)).toBeTruthy())
  it('reports multiple missing items',() => {
    const err = validators.password('abc')
    expect(err).toMatch(/8 characters/)
    expect(err).toMatch(/uppercase/)
  })
})

// ── required ──────────────────────────────────────────────────────────────────
describe('validators.required', () => {
  const req = validators.required('Name')
  it('accepts non-empty value',       () => expect(req('John')).toBeNull())
  it('rejects empty string',          () => expect(req('')).toMatch(/required/))
  it('rejects whitespace-only',       () => expect(req('   ')).toMatch(/required/))
  it('rejects null',                  () => expect(req(null)).toMatch(/required/))
  it('includes field name in error',  () => expect(req('')).toMatch(/Name/))
})

// ── minLength ─────────────────────────────────────────────────────────────────
describe('validators.minLength', () => {
  it('accepts meeting minimum',       () => expect(validators.minLength(3)('abc')).toBeNull())
  it('rejects below minimum',         () => expect(validators.minLength(3)('ab')).toBeTruthy())
})

// ── languageCode ─────────────────────────────────────────────────────────────
describe('validators.languageCode', () => {
  it('accepts valid BCP-47',          () => expect(validators.languageCode('en-US')).toBeNull())
  it('accepts hi-IN',                 () => expect(validators.languageCode('hi-IN')).toBeNull())
  it('rejects lowercase country',     () => expect(validators.languageCode('en-us')).toBeTruthy())
  it('rejects no hyphen',             () => expect(validators.languageCode('enus')).toBeTruthy())
  it('accepts null (optional field)', () => expect(validators.languageCode(null)).toBeNull())
})

// ── compose ───────────────────────────────────────────────────────────────────
describe('validators.compose', () => {
  const nameValidator = validators.compose(
    validators.required('Name'),
    validators.minLength(2, 'Name'),
    validators.maxLength(100, 'Name')
  )
  it('accepts valid name',            () => expect(nameValidator('Jo')).toBeNull())
  it('returns first error (required)',() => expect(nameValidator('')).toMatch(/required/))
  it('returns second error (min)',    () => expect(nameValidator('J')).toMatch(/2/))
})

// ── validateForm ─────────────────────────────────────────────────────────────
describe('validateForm', () => {
  const rules = { email: validators.email, password: validators.password }

  it('returns empty object for valid form', () => {
    const errs = validateForm({ email: 'a@b.com', password: 'TestPass1' }, rules)
    expect(errs).toEqual({})
  })

  it('returns errors for invalid form', () => {
    const errs = validateForm({ email: 'bad', password: 'weak' }, rules)
    expect(errs.email).toBeTruthy()
    expect(errs.password).toBeTruthy()
  })
})

// ── isValid ───────────────────────────────────────────────────────────────────
describe('isValid', () => {
  it('returns true for empty errors',     () => expect(isValid({})).toBe(true))
  it('returns false when errors present', () => expect(isValid({ email: 'bad' })).toBe(false))
})