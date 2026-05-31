import AppLayout    from '../components/layout/AppLayout'
import FileUploader from '../components/speech/FileUploader'
import { colors }   from '../utils/styles'

export default function UploadPage() {
  return (
    <AppLayout>
      <div style={{ maxWidth: 620, margin: '0 auto' }}>

        {/* Page header */}
        <div style={{ marginBottom: 28 }}>
          <h1 style={{
            fontSize: 24, fontWeight: 700,
            color: colors.gray[900],
            letterSpacing: '-0.02em', margin: '0 0 6px',
          }}>
            Upload audio
          </h1>
          <p style={{ fontSize: 14, color: colors.gray[500], margin: 0 }}>
            Upload an audio file to generate a transcript using AI speech recognition.
          </p>
        </div>

        {/* Upload card */}
        <div style={{
          background: '#fff',
          border: `1px solid ${colors.gray[200]}`,
          borderRadius: 16,
          padding: 24,
        }}>
          <FileUploader />
        </div>

        {/* Tips */}
        <div style={{
          marginTop: 20,
          padding: '14px 16px',
          background: colors.gray[50],
          border: `1px solid ${colors.gray[200]}`,
          borderRadius: 10,
        }}>
          <p style={{ fontSize: 13, fontWeight: 600, color: colors.gray[700], margin: '0 0 8px' }}>
            💡 Tips for better accuracy
          </p>
          <ul style={{
            fontSize: 13, color: colors.gray[500],
            margin: 0, paddingLeft: 18, lineHeight: 1.7,
          }}>
            <li>Use a clear recording with minimal background noise</li>
            <li>Select the correct language before uploading</li>
            <li>Set speaker count for meetings with multiple participants</li>
            <li>WAV and FLAC formats produce the best results</li>
          </ul>
        </div>
      </div>
    </AppLayout>
  )
}