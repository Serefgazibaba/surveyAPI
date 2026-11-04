import { api } from "./client.js";

/** GET /admin/getAllUsers?q&page&size → { items: [{ id, fullName, email }], page } */
export const listInvitableUsers = ({ q, page, size } = {}) =>
  api.get("/admin/getAllUsers", { query: { q, page, size } });
