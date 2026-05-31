import { colors, inputBase, label as labelStyle } from '../../utils/styles'

const LANGUAGES = [
  { code: 'en-US', name: 'English (US)' },
  { code: 'en-GB', name: 'English (UK)' },
  { code: 'hi-IN', name: 'Hindi'        },
  { code: 'es-ES', name: 'Spanish'      },
  { code: 'fr-FR', name: 'French'       },
  { code: 'de-DE', name: 'German'       },
  { code: 'ja-JP', name: 'Japanese'     },
  { code: 'zh-CN', name: 'Chinese'      },
  { code: 'ar-SA', name: 'Arabic'       },
  { code: 'pt-BR', name: 'Portuguese'   },
]

export default function LanguagePicker({ value, onChange, disabled = false, id = 'language' }) {
  return (
    <div>
      <label htmlFor={id} style={labelStyle}>Language</label>
      <select
        id={id}
        value={value}
        onChange={e => onChange(e.target.value)}
        disabled={disabled}
        style={{
          ...inputBase,
          cursor: disabled ? 'not-allowed' : 'pointer',
          maxWidth: 220,
        }}
      >
        {LANGUAGES.map(({ code, name }) => (
          <option key={code} value={code}>{name}</option>
        ))}
      </select>
    </div>
  )
}
