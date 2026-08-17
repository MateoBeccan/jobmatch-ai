import { useEffect, useRef } from 'react'

type ConfirmDialogProps = {
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({ open, title, message, confirmLabel = 'Eliminar', cancelLabel = 'Cancelar', onConfirm, onCancel }: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) {
      dialog.showModal()
    } else if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  function handleClose() {
    onCancel()
  }

  function handleBackdropClick(event: React.MouseEvent<HTMLDialogElement>) {
    if (event.target === dialogRef.current) onCancel()
  }

  return (
    <dialog ref={dialogRef} className="confirm-dialog" onClose={handleClose} onClick={handleBackdropClick}>
      <div className="confirm-dialog-content">
        <h2>{title}</h2>
        <p>{message}</p>
        <div className="confirm-dialog-actions">
          <button type="button" className="confirm-cancel" onClick={onCancel}>{cancelLabel}</button>
          <button type="button" className="confirm-delete" onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </dialog>
  )
}
