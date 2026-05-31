import { useUpload } from '../../hooks/useUpload'
import { useToast } from '../common/Toast'
import DropZone         from './DropZone'
import SelectedFile     from './SelectedFile'
import UploadOptions    from './UploadOptions'
import UploadProgress   from './UploadProgress'
import TranscriptResult from './TranscriptResult'
import ErrorMessage     from '../common/ErrorMessage'
import Button           from '../common/Button'
import { colors }       from '../../utils/styles'

/**
 * FileUploader — the main upload experience.
 *
 * State machine flow:
 *   idle → (drop/browse) → selected → (Upload button) →
 *   uploading → processing → done
 *                                  ↘ error (retry available)
 */
export default function FileUploader() {
  const upload = useUpload()
  const toast  = useToast()

  const handleFile = (file) => {
    upload.selectFile(file)
    toast.info(`${file.name} ready to upload`)
  }

  const handleUpload = async () => {
    await upload.upload()
    if (upload.isDone) {
      toast.success('Transcription complete!')
    }
  }

  // ── Render ──────────────────────────────────────────────────────────────────

  // Done — show result
  if (upload.isDone) {
    return <TranscriptResult result={upload.result} onUploadAnother={upload.reset} />
  }

  // Busy — uploading or processing
  if (upload.isBusy) {
    return (
      <UploadProgress
        progress={upload.progress}
        isProcessing={upload.isProcessing}
        filename={upload.file?.name}
      />
    )
  }

  // Idle / selected / error
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

      {/* Error banner from previous attempt */}
      {upload.isError && (
        <ErrorMessage
          message={upload.error}
          onRetry={upload.upload}
        />
      )}

      {/* Drop zone — shown when no file chosen */}
      {upload.isIdle && (
        <DropZone onFile={handleFile} />
      )}

      {/* Selected file card — shown when file is ready */}
      {upload.isSelected && (
        <SelectedFile
          file={upload.file}
          onRemove={upload.removeFile}
        />
      )}

      {/* Options — always visible when file is selected */}
      {(upload.isSelected || upload.isError) && upload.file && (
        <>
          <UploadOptions
            options={upload.options}
            onChange={upload.updateOptions}
          />

          {/* Upload button */}
          <Button
            variant="primary"
            fullWidth
            loading={upload.isBusy}
            onClick={upload.upload}
            style={{ marginTop: 4 }}
          >
            🚀 Transcribe audio
          </Button>

          {/* Or cancel / change file */}
          <button
            onClick={upload.reset}
            style={{
              background: 'none', border: 'none',
              color: colors.gray[400], fontSize: 13,
              cursor: 'pointer', textAlign: 'center',
              padding: '4px 0',
              textDecoration: 'underline',
            }}
          >
            Choose a different file
          </button>
        </>
      )}

      {/* Error retry — no file loaded */}
      {upload.isError && !upload.file && (
        <Button variant="secondary" fullWidth onClick={upload.reset}>
          Try again
        </Button>
      )}
    </div>
  )
}
