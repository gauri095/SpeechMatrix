import api from './api'

const SPEECH = '/api/speech'

const speechService = {

  /**
   * Upload an audio file for transcription.
   * @param {File}   file          audio file from <input type="file">
   * @param {string} language      BCP-47 code e.g. "en-US"
   * @param {number} speakerCount  optional number of speakers
   * @param {Function} onProgress  upload progress callback (0–100)
   */
  upload: (file, language = 'en-US', speakerCount = null, onProgress = null) => {
    const form = new FormData()
    form.append('file', file)
    form.append('language', language)
    if (speakerCount) form.append('speakerCount', speakerCount)

    return api.post(`${SPEECH}/upload`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
        ? (e) => onProgress(Math.round((e.loaded * 100) / e.total))
        : undefined,
      timeout: 120_000,  // 2 min — large files take time
    }).then(r => r.data.data)
  },

  /**
   * Send microphone recording bytes.
   * @param {Blob}   audioBlob  from MediaRecorder
   * @param {string} language   BCP-47 code
   * @param {string} mimeType   e.g. "audio/webm"
   */
  record: (audioBlob, language = 'en-US', mimeType = 'audio/webm') =>
    api.post(`${SPEECH}/record`, audioBlob, {
      headers: { 'Content-Type': mimeType },
      params: { language },
      timeout: 120_000,
    }).then(r => r.data.data),

  /**
   * Get paginated transcription history.
   * @param {object} params  { page, size, status, language, sortBy, sortDir }
   */
  getHistory: (params = {}) =>
    api.get(`${SPEECH}/history`, { params }).then(r => r.data.data),

  /**
   * Get a single transcription by ID.
   */
  getById: (id) =>
    api.get(`${SPEECH}/${id}`).then(r => r.data.data),

  /**
   * Search transcripts.
   * @param {string}  q      search query (min 2 chars)
   * @param {object}  params { page, size, paged }
   */
  search: (q, params = {}) =>
    api.get(`${SPEECH}/search`, { params: { q, ...params } })
       .then(r => r.data.data),

  /**
   * Get usage statistics.
   */
  getStats: () =>
    api.get(`${SPEECH}/stats`).then(r => r.data.data),

  /**
   * Delete a transcription.
   */
  delete: (id) =>
    api.delete(`${SPEECH}/${id}`).then(r => r.data),
}

export default speechService