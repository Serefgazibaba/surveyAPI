import { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import Button from "../../components/Button.jsx";
import Field from "../../components/Field.jsx";
import TextInput from "../../components/TextInput.jsx";
import Checkbox from "../../components/Checkbox.jsx";
import ComingSoon from "../../components/ComingSoon.jsx";
import ErrorAlert from "../../components/ErrorAlert.jsx";
import { useAuth } from "../../auth/AuthContext.jsx";
import { mapFieldErrors } from "../../utils/errorMessages.js";

/** Kilit süresini geri sayar (ACCOUNT_LOCKED + Retry-After). */
function useCountdown(seconds) {
  const [left, setLeft] = useState(seconds || 0);
  useEffect(() => { setLeft(seconds || 0); }, [seconds]);
  useEffect(() => {
    if (left <= 0) return;
    const t = setTimeout(() => setLeft(l => l - 1), 1000);
    return () => clearTimeout(t);
  }, [left]);
  return left;
}

const mmss = s => `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;

/** S-01 · Admin giriş */
export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "", remember: false });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const lockLeft = useCountdown(error?.code === "ACCOUNT_LOCKED" ? error.retryAfter : 0);
  const fieldErrors = mapFieldErrors(error);
  const target = location.state?.from || "/surveys";

  if (isAuthenticated) return <Navigate to={target} replace />;

  const submit = async e => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(form.email.trim(), form.password, form.remember);
      navigate(target, { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const set = key => e => setForm(f => ({ ...f, [key]: e.target.value }));
  const locked = lockLeft > 0;

  return <div className="login-screen">
    <section className="login-art">
      <div className="brand light"><div className="logo">S</div><div><b>Survey Tool</b><small>DefineX</small></div></div>
      <div><h2>Anketlerinizi dakikalar içinde hazırlayın, güvenle dağıtın.</h2><p>Kurum içi ve kurum dışı anketler için kişiye özel linkler, anlık katılım takibi ve tek tıkla raporlama.</p></div>
      <small>© 2026 DefineX</small>
    </section>
    <section className="login-form">
      <form onSubmit={submit} noValidate>
        <div><h1>Giriş Yap</h1><p>Admin hesabınızla devam edin.</p></div>
        <ErrorAlert error={error} hideItems extra={locked && <p><b style={{ display: "inline" }}>Kalan süre: {mmss(lockLeft)}</b></p>} />
        <ComingSoon block><Button icon="lock" disabled style={{ width: "100%" }}>Kurumsal hesapla giriş (SSO)</Button></ComingSoon>
        <div className="or">veya</div>
        <Field label="E-posta" error={fieldErrors.email}>
          <TextInput type="email" autoComplete="username" maxLength={254} value={form.email} onChange={set("email")} required />
        </Field>
        <Field label="Şifre" error={fieldErrors.password}>
          <TextInput type="password" autoComplete="current-password" maxLength={128} value={form.password} onChange={set("password")} required />
        </Field>
        <div className="inline-between muted">
          <Checkbox label="Beni hatırla" checked={form.remember} onChange={v => setForm(f => ({ ...f, remember: v }))} />
          <ComingSoon><button type="button" className="link-btn" disabled>Şifremi unuttum</button></ComingSoon>
        </div>
        <Button variant="primary" type="submit" loading={busy} disabled={locked || !form.email || !form.password}>Giriş Yap</Button>
      </form>
    </section>
  </div>;
}
