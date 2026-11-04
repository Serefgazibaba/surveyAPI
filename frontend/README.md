# Survey Tool · Ön Yüz (surveyX)

React 19 + Vite ön yüzü. Yalnızca `surveyAPI` backend uçlarını kullanır (backend'de değişiklik yapılmaz).

## Çalıştırma
```bash
npm install
npm run dev        # http://localhost:3000 (davet linkleri {FRONTEND_BASE_URL}/s/{token} ile uyumlu)
```
- `/api` istekleri Vite proxy ile `http://localhost:8080` adresine gider (`VITE_PROXY_TARGET`). Proxy `Origin` başlığını kaldırır; CORS ayarı gerekmez.
- 3000 portu doluysa (örn. backend compose'undaki `frontend` container'ı): `npx vite --port 3001`.
- `npm run lint`, `npm run build`.

## Yapı
- `src/components/` — her reusable bileşen ayrı dosyada (Button, Field, DataTable, Modal, ErrorAlert…). Sayfalar yalnızca bunları birleştirir.
- `src/api/` — `client.js` (fetch, Bearer token, tekil refresh, problem+json → `ApiError`) + domain modülleri.
- `src/utils/errorMessages.js` — backend `code` → Türkçe başlık/açıklama; alan yolları → okunur etiket. Tüm hata gösterimi `ErrorAlert`, `ErrorState`, `FieldError`, `Toast`, `ErrorBoundary` üzerinden.
- `src/pages/admin/` — giriş, anket listesi, oluşturma/düzenleme, katılımcı seçimi ve yayın, gönderim durumu, şablonlar (liste, düzenleme, şablondan anket oluşturma; builder'da "Şablon olarak kaydet"), raporlar (`/reports` liste, `/reports/:surveyId` detay: KPI, soru dağılımları, metin cevapları, kayıtlı modda katılımcı yanıtları, CSV indirme, PDF için yazdırma). Soru listesi `QuestionList` anket ve şablon builder'larında ortaktır.
- `src/pages/participant/` — `/s/:token` davet girişi, form (taslak + otomatik kayıt), teşekkür, erişim hatası.
- `docs/prototype-App.jsx` — orijinal tıklanabilir prototip (referans).

Backend'de karşılığı olmayan özellikler (Bölümler, katılımcı CSV içe aktarımı, SSO, şifre sıfırlama) "Yakında" olarak pasif gösterilir.
