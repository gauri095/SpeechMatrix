import { colors, inputBase, label } from '../../utils/styles'

const LANGUAGES = [
  { code: 'en-US', name: 'English (US)' },
  { code: 'en-GB', name: 'English (UK)' },
  { code: 'hi-IN', name: 'Hindi' },
  { code: 'es-ES', name: 'Spanish' },
  { code: 'fr-FR', name: 'French' },
  { code: 'de-DE', name: 'German' },
  { code: 'ja-JP', name: 'Japanese' },
  { code: 'zh-CN', name: 'Chinese (Mandarin)' },
  { code: 'ar-SA', name: 'Arabic' },
  { code: 'pt-BR', name: 'Portuguese (Brazil)' },
]

/**
 * UploadOptions — language and speaker count selectors.
 * Rendered between the drop zone and the upload button.
 */
export default function UploadOptions({ options, onChange, disabled = false }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>

      {/* Language */}
      <div>
        <label htmlFor="upload-language" style={label}>
          Language
        </label>
        <select
          id="upload-language"
          value={options.language}
          onChange={(e) => onChange({ language: e.target.value })}
          disabled={disabled}
          style={{
            ...inputBase,
            cursor: disabled ? 'not-allowed' : 'pointer',
          }}
        >
          {LANGUAGES.map(({ code, name }) => (
            <option key={code} value={code}>{name}</option>
          ))}
        </select>
      </div>

      {/* Speaker count */}
      <div>
        <label htmlFor="upload-speakers" style={label}>
          Speakers
        </label>
        <select
          id="upload-speakers"
          value={options.speakerCount ?? ''}
          onChange={(e) => onChange({
            speakerCount: e.target.value ? Number(e.target.value) : null
          })}
          disabled={disabled}
          style={{
            ...inputBase,
            cursor: disabled ? 'not-allowed' : 'pointer',
          }}
        >
          <option value="">Auto-detect</option>
          <option value="1">1 speaker</option>
          <option value="2">2 speakers</option>
          <option value="3">3 speakers</option>
          <option value="4">4+ speakers</option>
        </select>
      </div>
    </div>
  )
}
