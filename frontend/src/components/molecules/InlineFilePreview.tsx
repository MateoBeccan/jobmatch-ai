import { useObjectUrl } from '../../lib/hooks/useObjectUrl'

type InlineFilePreviewProps = {
  file: File | null
  type: 'pdf' | 'image'
  title: string
}

export function InlineFilePreview({ file, type, title }: InlineFilePreviewProps) {
  const previewUrl = useObjectUrl(file)

  if (!file || !previewUrl) return null

  return (
    <div className={`inline-file-preview ${type === 'pdf' ? 'inline-pdf-preview' : 'inline-image-preview'}`}>
      {type === 'pdf' ? (
        <iframe src={previewUrl} title={title} />
      ) : (
        <img src={previewUrl} alt={`Vista previa de ${file.name}`} />
      )}
    </div>
  )
}
