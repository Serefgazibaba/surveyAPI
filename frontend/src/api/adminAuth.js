import { api } from "./client.js";

/** POST /admin/auth/login → AdminAuthTokenResponse */
export const adminLogin = (email, password) => api.post("/admin/auth/login", { email, password }, { auth: false });

/** POST /admin/auth/logout → 204 (idempotent) */
export const adminLogout = () => api.post("/admin/auth/logout", undefined);
