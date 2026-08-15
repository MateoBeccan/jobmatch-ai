import type { DragEvent } from 'react'
import { formatFileSize } from '../../lib/helpers/format'

export type UploadedFile = {
  name: string
  size: number
}

type FileUploadCardProps = {
  file: UploadedFile | null
  isDragging: boolean
  onChange: () => void
  onRemove: () => void
  onDrop: (event: DragEvent<HTMLButtonElement>) => void
  onDragOver: (event: DragEvent<HTMLButtonElement>) => void
  onDragLeave: () => void
}

export function FileUploadCard({ file, isDragging, onChange, onRemove, onDrop, onDragOver, onDragLeave }: FileUploadCardProps) {
  if (file) {
    return (
      <div className="file-card has-file">
        <span className="file-check" aria-hidden="true">✓</span>
        <span className="file-copy">
          <strong>{file.name}</strong>
          <small>{formatFileSize(file.size)} · PDF</small>
          <span className="file-status">CV cargado</span>
        </span>
        <span className="file-actions">
          <button className="file-action-button" type="button" onClick={onChange}>Cambiar archivo</button>
          <button className="file-action-button danger" type="button" onClick={onRemove} aria-label="Eliminar archivo">Eliminar</button>
        </span>
      </div>
    )
  }

  return (
    <button
      className={`dropzone ${isDragging ? 'is-dragging' : ''}`}
      type="button"
      onClick={onChange}
      onDragOver={onDragOver}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
    >
      <span className="upload-icon" aria-hidden="true">↥</span>
      <span className="dropzone-copy">
        <strong>Arrastrá tu CV acá</strong>
        <small>o hacé clic para seleccionar el archivo</small>
      </span>
      <span className="dropzone-empty-meta">PDF · hasta 5 MB</span>
      <span className="browse-label">Seleccionar archivo</span>
    </button>
  )
}
