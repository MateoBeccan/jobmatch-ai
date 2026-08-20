import type { DragEvent } from 'react'
import { formatFileSize } from '../../lib/helpers/format'

export type UploadedFile = {
  name: string
  size: number
}

type FileUploadCardProps = {
  file: UploadedFile | null
  isDragging: boolean
  errorMessage?: string | null
  onChange: () => void
  onRemove: () => void
  onDrop: (event: DragEvent<HTMLButtonElement>) => void
  onDragOver: (event: DragEvent<HTMLButtonElement>) => void
  onDragLeave: () => void
}

export function FileUploadCard({
  file,
  isDragging,
  errorMessage = null,
  onChange,
  onRemove,
  onDrop,
  onDragOver,
  onDragLeave,
}: FileUploadCardProps) {
  if (file) {
    return (
      <div className="file-card cv-file-card has-file">
        <span className="file-check" aria-hidden="true">✓</span>
        <span className="file-copy">
          <strong>{file.name}</strong>
          <small>{formatFileSize(file.size)} · PDF</small>
          <span className="file-status">CV listo</span>
        </span>
        <span className="file-actions">
          <button className="file-action-button" type="button" onClick={onChange}>Cambiar archivo</button>
          <button className="file-action-button danger" type="button" onClick={onRemove} aria-label="Eliminar archivo">Eliminar</button>
        </span>
      </div>
    )
  }

  const stateClass = errorMessage ? 'has-error' : isDragging ? 'is-dragging' : ''
  const title = errorMessage ? 'No pudimos cargar este archivo' : isDragging ? 'Soltá tu CV para cargarlo' : 'Arrastrá tu CV acá'
  const description = errorMessage ? errorMessage : isDragging ? 'PDF detectado' : 'o hacé clic para seleccionarlo'
  const actionLabel = errorMessage ? 'Intentar nuevamente' : 'Seleccionar archivo'

  return (
    <button
      className={`dropzone ${stateClass}`}
      type="button"
      onClick={onChange}
      onDragOver={onDragOver}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
    >
      <span className="upload-icon" aria-hidden="true">{errorMessage ? '!' : '↥'}</span>
      <span className="dropzone-copy">
        <strong>{title}</strong>
        <small>{description}</small>
      </span>
      <span className="dropzone-empty-meta">PDF · máximo 5 MB</span>
      <span className="browse-label">{actionLabel}</span>
    </button>
  )
}
