import { useEffect, useState } from 'react'
import { Modal, Button, Alert } from 'react-bootstrap'

/**
 * In-app replacement for window.confirm()/alert() on destructive actions — the native pair renders
 * as "localhost says…", can't be styled, and blocks the tab. Same reasoning as CheckoutModal, but
 * for the plain yes/no case where there is nothing extra to collect.
 *
 * onConfirm may be async. A rejection is shown inline and the modal stays open so the action can be
 * retried; the modal only closes once onConfirm resolves.
 */
export default function ConfirmModal({
  show,
  title = 'Please confirm',
  body,
  confirmLabel = 'Confirm',
  confirmVariant = 'danger',
  onClose,
  onConfirm,
}) {
  const [error, setError] = useState('')
  const [working, setWorking] = useState(false)

  // Clear a previous failure so a reopened modal doesn't show a stale message.
  useEffect(() => { if (show) { setError(''); setWorking(false) } }, [show])

  const confirm = async () => {
    setWorking(true); setError('')
    try {
      await onConfirm()
      onClose()
    } catch (err) {
      setError(err.message)
    } finally { setWorking(false) }
  }

  return (
    <Modal show={show} onHide={working ? undefined : onClose} centered>
      <Modal.Header closeButton={!working}><Modal.Title>{title}</Modal.Title></Modal.Header>
      <Modal.Body>
        {error && <Alert variant="danger" className="py-2">{error}</Alert>}
        {body}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onClose} disabled={working}>Keep it</Button>
        <Button variant={confirmVariant} onClick={confirm} disabled={working}>
          {working ? 'Working…' : confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  )
}
