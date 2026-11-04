import { api } from "./client.js";

/** GET /admin/templates?q&page&size → { items, page } (yalnızca kendi şablonları) */
export const listTemplates = ({ q, page, size } = {}) =>
  api.get("/admin/templates", { query: { q, page, size } });

/** GET /admin/templates/{id} → TemplateDetail */
export const getTemplate = id => api.get(`/admin/templates/${id}`);

/** POST /admin/upsertTemplate (id yoksa oluşturur; questions içeriğin tamamıdır) → TemplateDetail */
export const upsertTemplate = payload => api.post("/admin/upsertTemplate", payload);

/** DELETE /admin/templates/{id} → 204 (şablondan üretilmiş anketler etkilenmez) */
export const deleteTemplate = id => api.del(`/admin/templates/${id}`);

/** POST /admin/saveAsTemplate { surveyId, name?, description? } → TemplateDetail */
export const saveAsTemplate = ({ surveyId, name, description }) =>
  api.post("/admin/saveAsTemplate", { surveyId, name, description });

/** POST /admin/createSurveyFromTemplate { templateId, title? } → SurveyDetailResponse (yeni DRAFT) */
export const createSurveyFromTemplate = (templateId, title) =>
  api.post("/admin/createSurveyFromTemplate", { templateId, title });
