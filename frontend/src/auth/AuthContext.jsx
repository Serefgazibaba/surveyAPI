import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { tokenStore } from "./tokenStore.js";
import { onAdminSessionExpired } from "../api/client.js";
import { adminLogin, adminLogout } from "../api/adminAuth.js";
import { useToast } from "../components/Toast.jsx";

const AuthContext = createContext(null);

/** Admin oturumu. Katılımcı oturumu ayrı tutulur (tokenStore.participant). */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => tokenStore.getAdmin()?.user ?? null);
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  useEffect(() => {
    onAdminSessionExpired(() => {
      setUser(null);
      toast.error("Oturumunuz sona erdi. Devam etmek için tekrar giriş yapın.");
      navigate("/login", { replace: true, state: { from: location.pathname + location.search } });
    });
  }, [navigate, location, toast]);

  const login = useCallback(async (email, password, remember) => {
    const data = await adminLogin(email, password);
    tokenStore.setAdmin(data, remember);
    setUser(data.user);
    return data.user;
  }, []);

  const logout = useCallback(async () => {
    try { await adminLogout(); } catch { /* oturum zaten bitmiş olabilir */ }
    tokenStore.clearAdmin();
    setUser(null);
    navigate("/login", { replace: true });
  }, [navigate]);

  const value = useMemo(() => ({ user, isAuthenticated: !!user && !!tokenStore.getAdmin(), login, logout }), [user, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
