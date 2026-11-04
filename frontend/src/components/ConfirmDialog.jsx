import Modal from "./Modal.jsx";
import Button from "./Button.jsx";
import ErrorAlert from "./ErrorAlert.jsx";

/**
 * Onay penceresi. onConfirm async olabilir; hata olursa `error` ile modal içinde gösterilir.
 * danger: onay butonu kırmızı.
 */
export default function ConfirmDialog({ title, message, confirmLabel = "Onayla", danger = false, busy = false, error, onConfirm, onCancel }) {
  return <Modal title={title} onClose={onCancel} busy={busy} footer={<>
    <Button onClick={onCancel} disabled={busy}>Vazgeç</Button>
    <Button variant={danger ? "danger" : "primary"} loading={busy} onClick={onConfirm}>{confirmLabel}</Button>
  </>}>
    <ErrorAlert error={error} />
    <p className="text-muted" style={{ margin: 0 }}>{message}</p>
  </Modal>;
}
