import { api } from "./client.js";

/** GET /admin/getMails → { items, summary, page } */
export const listMails = ({ emailStatus, invitationStatus, surveyId, q, page, size } = {}) =>
  api.get("/admin/getMails", { query: { emailStatus, invitationStatus, surveyId, q, page, size } });

/** POST /admin/resendMails { emailIds } → { requeuedCount, items } (hepsi ya da hiç) */
export const resendMails = emailIds => api.post("/admin/resendMails", { emailIds });
