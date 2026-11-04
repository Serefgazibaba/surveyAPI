import { useState } from "react";
import Modal from "../../components/Modal.jsx";
import Button from "../../components/Button.jsx";
import Field from "../../components/Field.jsx";
import TextInput from "../../components/TextInput.jsx";
import TextArea from "../../components/TextArea.jsx";
import ErrorAlert from "../../components/ErrorAlert.jsx";
import { mapFieldErrors } from "../../utils/errorMessages.js";

/**
 * "Şablon olarak kaydet": ad ve açıklama (varsayılan: anketinkiler) sorulur.
 * onSave({ name, description }) async; hata modal içinde gösterilir.
 */
export default function SaveAsTemplateModal({ defaultName, defaultDescription, onSave, onClose }) {
  const [name, setName] = useState(defaultName || "");
  const [description, setDescription] = useState(defaultDescription || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const fieldErrors = mapFieldErrors(error);

  const submit = async e => {
    e?.preventDefault();
    if (!name.trim()) {
      setError({ status: 400, code: "VALIDATION_ERROR", errors: [{ field: "name", code: "NotBlank", message: "Şablon adı boş bırakılamaz" }] });
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSave({ name: name.trim(), description: description.trim() || undefined });
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return <Modal title="Şablon olarak kaydet" subtitle="Anketin soruları şablona kopyalanır; katılımcılar, bitiş tarihi ve yanıt modu alınmaz."
    onClose={onClose} busy={busy}
    footer={<>
      <Button onClick={onClose} disabled={busy}>Vazgeç</Button>
      <Button variant="primary" icon="template" loading={busy} onClick={submit}>Şablonu Kaydet</Button>
    </>}>
    <form onSubmit={submit}>
      <ErrorAlert error={error} />
      <Field label="Şablon adı" id="name" error={fieldErrors.name}>
        <TextInput autoFocus maxLength={200} value={name} onChange={e => setName(e.target.value)} placeholder="Örn. Eğitim Değerlendirme" />
      </Field>
      <Field label="Açıklama" id="description" error={fieldErrors.description}>
        <TextArea maxLength={2000} value={description} onChange={e => setDescription(e.target.value)} placeholder="Şablonun ne için kullanıldığı" />
      </Field>
    </form>
  </Modal>;
}
