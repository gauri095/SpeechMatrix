import { describe, it, expect } from 'vitest'
import {
  formatDuration, formatConfidence, formatWordCount,
  formatFileSize, formatDurationLong, getStatusBadge, getLanguageName
} from '../utils/formatters'

describe('formatDuration', () => {
  it('formats seconds under a minute', () => expect(formatDuration(45)).toBe('0:45'))
  it('formats one minute exactly',     () => expect(formatDuration(60)).toBe('1:00'))
  it('formats minutes and seconds',    () => expect(formatDuration(125)).toBe('2:05'))
  it('formats hours correctly',        () => expect(formatDuration(3661)).toBe('1:01:01'))
  it('handles null/zero',              () => expect(formatDuration(0)).toBe('0:00'))
})

describe('formatConfidence', () => {
  it('formats 0.9872 as 98.7%', () => expect(formatConfidence(0.9872)).toBe('98.7%'))
  it('formats 1.0 as 100.0%',   () => expect(formatConfidence(1.0)).toBe('100.0%'))
  it('handles null',            () => expect(formatConfidence(null)).toBe('—'))
})

describe('formatWordCount', () => {
  it('adds comma separator for large numbers',   () => expect(formatWordCount(1234)).toBe('1,234 words'))
  it('uses singular for 1',                      () => expect(formatWordCount(1)).toBe('1 word'))
  it('returns dash for zero',                    () => expect(formatWordCount(0)).toBe('—'))
})

describe('formatFileSize', () => {
  it('formats bytes',     () => expect(formatFileSize(512)).toBe('512 B'))
  it('formats KB',        () => expect(formatFileSize(1536)).toBe('1.5 KB'))
  it('formats MB',        () => expect(formatFileSize(5242880)).toBe('5 MB'))
})

describe('formatDurationLong', () => {
  it('formats minutes',           () => expect(formatDurationLong(300)).toBe('5 mins'))
  it('formats singular minute',   () => expect(formatDurationLong(60)).toBe('1 min'))
  it('formats hours and minutes', () => expect(formatDurationLong(3660)).toBe('1 hr 1 min'))
})

describe('getStatusBadge', () => {
  it('done → green',       () => expect(getStatusBadge('done').color).toBe('green'))
  it('failed → red',       () => expect(getStatusBadge('failed').color).toBe('red'))
  it('processing → yellow',() => expect(getStatusBadge('processing').color).toBe('yellow'))
  it('unknown → gray',     () => expect(getStatusBadge('xyz').color).toBe('gray'))
})

describe('getLanguageName', () => {
  it('returns full name for known code',   () => expect(getLanguageName('en-US')).toBe('English (US)'))
  it('returns code for unknown',           () => expect(getLanguageName('xx-YY')).toBe('xx-YY'))
})