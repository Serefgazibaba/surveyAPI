import { api } from "./client.js";

/** GET /admin/reports?q&status[]&page&size → { items, page } (taslak olmayan kendi anketleri) */
export const listReports = ({ q, status, page, size } = {}) =>
  api.get("/admin/reports", { query: { q, status, page, size } });

/** GET /admin/reports/{surveyId} → SurveyReport (özet, soru dağılımları, metinler, kayıtlı modda katılımcı yanıtları) */
export const getReport = surveyId => api.get(`/admin/reports/${surveyId}`);

/** GET /admin/reports/{surveyId}/download → CSV dosyası; tarayıcıda indirme başlatır. */
export async function downloadReportCsv(surveyId, fallbackName = "rapor") {
  const { blob, filename } = await api.get(`/admin/reports/${surveyId}/download`, { responseType: "blob" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename || `${fallbackName}-yanitlar.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
