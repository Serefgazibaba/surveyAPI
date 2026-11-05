import { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import Button from "../../components/Button.jsx";
import Field from "../../components/Field.jsx";
import TextInput from "../../components/TextInput.jsx";
import Checkbox from "../../components/Checkbox.jsx";
import ComingSoon from "../../components/ComingSoon.jsx";
import ErrorAlert from "../../components/ErrorAlert.jsx";
import Icon from "../../components/Icon.jsx";
import BrandLogo from "../../components/BrandLogo.jsx";
import LoginScene from "./LoginScene.jsx";
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
  const [showPassword, setShowPassword] = useState(false);
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

  const passwordToggle = <button type="button" className="password-toggle" onClick={() => setShowPassword(s => !s)}
    aria-label={showPassword ? "Şifreyi gizle" : "Şifreyi göster"} aria-pressed={showPassword}>
    <Icon name={showPassword ? "eyeOff" : "eye"} />
  </button>;

  return <div className="login-screen">
    <section className="login-scene">
      <BrandLogo className="login-scene-logo" size={30} />
      <LoginScene />
    </section>
    <section className="login-form">
      <form onSubmit={submit} noValidate>
        <BrandLogo className="login-form-logo" size={28} />
        <div><h1>Giriş yap</h1><p>Admin hesabınızla devam edin.</p></div>
        <ErrorAlert error={error} hideItems extra={locked && <p><b style={{ display: "inline" }}>Kalan süre: {mmss(lockLeft)}</b></p>} />
        <Field label="E-posta" error={fieldErrors.email}>
          <TextInput type="email" autoComplete="username" maxLength={254} placeholder="ad.soyad@definex.com" value={form.email} onChange={set("email")} required />
        </Field>
        <Field label="Şifre" error={fieldErrors.password}>
          <TextInput type={showPassword ? "text" : "password"} autoComplete="current-password" maxLength={128} value={form.password} onChange={set("password")} required suffix={passwordToggle} />
        </Field>
        <Checkbox label="Beni hatırla" checked={form.remember} onChange={v => setForm(f => ({ ...f, remember: v }))} />
        <Button variant="primary" type="submit" loading={busy} disabled={locked || !form.email || !form.password}>Giriş yap</Button>
        <div className="or">veya</div>
        <ComingSoon block><Button icon="lock" disabled style={{ width: "100%" }}>Kurumsal hesapla giriş (SSO)</Button></ComingSoon>
        <small className="login-copyright">© 2026 DefineX</small>
      </form>
    </section>
  </div>;
}
