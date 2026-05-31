import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useUpload } from '../hooks/useUpload'
import speechService from '../services/speechService'

vi.mock('../services/speechService')

const mockFile = new File(['audio data'], 'test.mp3', { type: 'audio/mpeg' })
const mockResult = {
  id: 1, transcript: 'Hello world', confidence: 0.97,
  wordCount: 2, durationSeconds: 5.0, language: 'en-US',
}

beforeEach(() => vi.clearAllMocks())

describe('useUpload initial state', () => {
  it('starts idle', () => {
    const { result } = renderHook(() => useUpload())
    expect(result.current.state).toBe('idle')
    expect(result.current.isIdle).toBe(true)
    expect(result.current.file).toBeNull()
    expect(result.current.result).toBeNull()
  })
})

describe('selectFile()', () => {
  it('transitions to selected with the file set', () => {
    const { result } = renderHook(() => useUpload())
    act(() => result.current.selectFile(mockFile))
    expect(result.current.state).toBe('selected')
    expect(result.current.isSelected).toBe(true)
    expect(result.current.file?.name).toBe('test.mp3')
  })

  it('clears previous error on new file', () => {
    const { result } = renderHook(() => useUpload())
    act(() => { result.current.selectFile(mockFile) })
    act(() => { result.current.selectFile(mockFile) })
    expect(result.current.error).toBeNull()
  })
})

describe('upload() — success path', () => {
  it('ends in done state with result', async () => {
    speechService.upload.mockResolvedValue(mockResult)
    const { result } = renderHook(() => useUpload())
    act(() => result.current.selectFile(mockFile))

    await act(async () => { await result.current.upload() })

    expect(result.current.state).toBe('done')
    expect(result.current.isDone).toBe(true)
    expect(result.current.result).toEqual(mockResult)
  })
})

describe('upload() — failure path', () => {
  it('ends in error state with message', async () => {
    const err = Object.assign(new Error(), { apiMessage: 'File format not supported' })
    speechService.upload.mockRejectedValue(err)
    const { result } = renderHook(() => useUpload())
    act(() => result.current.selectFile(mockFile))

    await act(async () => { await result.current.upload() })

    expect(result.current.state).toBe('error')
    expect(result.current.isError).toBe(true)
    expect(result.current.error).toBe('File format not supported')
  })
})

describe('reset()', () => {
  it('returns to idle and clears all state', async () => {
    speechService.upload.mockResolvedValue(mockResult)
    const { result } = renderHook(() => useUpload())
    act(() => result.current.selectFile(mockFile))
    await act(async () => { await result.current.upload() })
    expect(result.current.isDone).toBe(true)

    act(() => result.current.reset())

    expect(result.current.state).toBe('idle')
    expect(result.current.file).toBeNull()
    expect(result.current.result).toBeNull()
    expect(result.current.progress).toBe(0)
  })
})

describe('removeFile()', () => {
  it('returns to idle without clearing options', () => {
    const { result } = renderHook(() => useUpload())
    act(() => result.current.selectFile(mockFile))
    act(() => result.current.updateOptions({ language: 'hi-IN' }))
    act(() => result.current.removeFile())

    expect(result.current.state).toBe('idle')
    expect(result.current.file).toBeNull()
    expect(result.current.options.language).toBe('hi-IN') // preserved
  })
})

describe('updateOptions()', () => {
  it('merges options correctly', () => {
    const { result } = renderHook(() => useUpload())
    act(() => result.current.updateOptions({ language: 'fr-FR', speakerCount: 2 }))
    expect(result.current.options.language).toBe('fr-FR')
    expect(result.current.options.speakerCount).toBe(2)
  })

  it('partial update does not overwrite other options', () => {
    const { result } = renderHook(() => useUpload())
    act(() => result.current.updateOptions({ language: 'es-ES' }))
    act(() => result.current.updateOptions({ speakerCount: 3 }))
    expect(result.current.options.language).toBe('es-ES') // preserved
    expect(result.current.options.speakerCount).toBe(3)
  })
})

describe('isBusy', () => {
  it('is true while uploading', async () => {
    let resolveUpload
    speechService.upload.mockImplementation(
      () => new Promise(res => { resolveUpload = res })
    )
    const { result } = renderHook(() => useUpload())
    act(() => result.current.selectFile(mockFile))

    act(() => { result.current.upload() })
    expect(result.current.isBusy).toBe(true)

    await act(async () => { resolveUpload(mockResult) })
    expect(result.current.isBusy).toBe(false)
  })
})