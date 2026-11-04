# Survey API

Survey Tool backend'i. Admin (anket sahibi) anket oluşturur, yayınlar ve raporlar. Katılımcı (User) e-postadaki
davet linkiyle anketi doldurur. API sözleşmesi: [`docs/openapi/survey-api-docs-grup1.v1.yaml`](docs/openapi/survey-api-docs-grup1.v1.yaml).

## Teknoloji

Java 21 · Spring Boot 3.5 · Maven (wrapper) · Spring Web, Validation, Data JPA, Security (OAuth2 Resource Server / JWT) · PostgreSQL 16 · springdoc-openapi (Swagger UI, yalnızca dev)

## Hızlı başlangıç (tek komut)

Gereksinim: Docker.

```bash
cp .env.example .env          # değerleri doldurun (özellikle DB_PASSWORD ve JWT_SECRET)
docker compose up --build
```

PostgreSQL ve API birlikte ayağa kalkar. API adresi: `http://localhost:8080/api/v1`.

| Komut | Ne yapar |
|---|---|
| `docker compose up --build` | DB + API'yi derleyip başlatır |
| `docker compose up -d postgres` | Yalnızca DB (API'yi IDE'den çalıştırmak için) |
| `docker compose down` | Durdurur (veri korunur) |
| `docker compose down -v` | Durdurur ve DB verisini siler |

## Swagger UI (yalnızca dev)

Uygulama `dev` profiliyle çalışırken (docker compose ve yerel çalıştırmada varsayılan) Swagger UI açıktır:

- Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/v1/v3/api-docs

Korumalı uçları denemek için önce login ucundan access token alın, ardından sağ üstteki **Authorize** butonuna token'ı
girin (`Bearer` yazmadan). Doküman koddaki controller'lardan üretilir; sözleşmenin asıl kaynağı
`docs/openapi/` altındaki YAML'dır. `dev` dışındaki profillerde Swagger UI ve `/v3/api-docs` kapalıdır.

## IDE / komut satırından çalıştırma

Gereksinim: JDK 21 (`JAVA_HOME`). Maven kurmanız gerekmez, `mvnw` kendisi indirir.

```bash
docker compose up -d postgres
export $(grep -v '^#' .env | xargs)      # Windows PowerShell: .env değerlerini ortam değişkeni olarak tanımlayın
./mvnw spring-boot:run                    # Windows: mvnw.cmd spring-boot:run
```

IntelliJ'de çalıştırırken Run Configuration → Environment variables alanına `.env` değerlerini girin.

## Environment variable'lar

| Değişken | Zorunlu | Varsayılan | Açıklama |
|---|---|---|---|
| `DB_URL` | | `jdbc:postgresql://localhost:5432/survey` | JDBC adresi |
| `DB_USERNAME` | | `survey` | DB kullanıcısı |
| `DB_PASSWORD` | ✔ | | DB şifresi |
| `JWT_SECRET` | ✔ | | HS256 imza anahtarı, en az 32 karakter (`openssl rand -base64 48`) |
| `CORS_ALLOWED_ORIGINS` | | `http://localhost:3000` | Virgülle ayrılmış izinli origin'ler |
| `FRONTEND_BASE_URL` | | `http://localhost:3000` | Davet linklerinin tabanı (`{FRONTEND_BASE_URL}/s/{token}`) |
| `MAIL_HOST`, `MAIL_PORT` | | `smtp.gmail.com`, `587` | SMTP sunucusu (STARTTLS) |
| `MAIL_USERNAME` | mail için | | SMTP kullanıcısı |
| `MAIL_PASSWORD` | mail için | | SMTP şifresi (Gmail: uygulama şifresi) |
| `MAIL_FROM`, `MAIL_FROM_NAME` | | `MAIL_USERNAME`, `Survey Tool` | Gönderen adresi ve adı |
| `SERVER_PORT` | | `8080` | HTTP portu |
| `DB_NAME`, `DB_PORT` | | `survey`, `5432` | Yalnızca docker-compose için |
| `AUTH_SESSION_IDLE_TIMEOUT` | | `30m` | Son refresh'ten sonra oturumun düşeceği süre (FR-02) |
| `AUTH_MAX_FAILED_ATTEMPTS_PER_IP` | | `5` | Aynı e-posta + IP için kilit eşiği (FR-01) |
| `AUTH_MAX_FAILED_ATTEMPTS_PER_ACCOUNT` | | `20` | Bir e-posta için tüm IP'lerden toplam kilit eşiği |

Gizli bilgiler koda veya `application*.yml` dosyalarına yazılmaz. `.env` git'e eklenmez.

## Proje yapısı

```
com.definex.survey
├─ config/      @ConfigurationProperties (JWT, Auth, CORS), CORS ve Clock bean'leri, OpenAPI
├─ security/    SecurityFilterChain, JWT encoder/decoder, JwtTokenService, Roles, 401/403 handler'ları
├─ exception/   GlobalExceptionHandler, ProblemDetailFactory, ApiException ve alt sınıfları
├─ mail/        e-posta altyapısı: email_messages, JavaMailSender, MailService (anket/davet bilmez)
├─ common/      modüllerden bağımsız yardımcılar; entity/BaseEntity (UUID id, createdAt, updatedAt), dto/PageMeta
├─ auth/        /admin/auth/*, /user/auth/*, /admin/getAllUsers
├─ survey/      /admin/surveys/*, /admin/upsertSurvey, /admin/publishSurvey, /admin/getMails, /admin/resendMails
├─ report/      /admin/surveys/{surveyId}/report*
└─ user/        katılımcı tarafı: /user/surveys/* (form, ara kayıt, yanıt gönderimi)
```

Her iş modülü aynı alt paketleri taşır: `controller/ service/ repository/ entity/ dto/`.

## Geliştirme kuralları (paralel çalışma için)

**Modül sınırları**
- Her ekip/geliştirici kendi modül klasöründe çalışır. Ortak paketlere (`config`, `security`, `exception`, `common`)
  yapılan değişiklikler ayrı PR ile ve ekip onayıyla yapılır.
- Bir modül başka bir modülün `repository` veya `entity` sınıfını doğrudan kullanmaz; o modülün `service`'ini çağırır.

**Katmanlar**
- `controller`: yalnızca HTTP eşlemesi ve `@Valid`. İş mantığı yok, entity döndürmez; giriş/çıkış DTO'dur.
- `service`: iş mantığı, sahiplik kontrolü (Admin yalnızca kendi anketine erişir, değilse `NotFoundException`) ve `@Transactional`.
- `repository`: Spring Data JPA arayüzleri.
- `dto`: Java `record`; doğrulama anotasyonları burada.

**İsimlendirme**

| Tür | Kalıp | Örnek |
|---|---|---|
| Controller | `<Grup><Kaynak>Controller` | `AdminSurveyController`, `UserSurveyController` |
| Service | `<Kaynak>Service` | `SurveyService` |
| Repository | `<Entity>Repository` | `SurveyRepository` |
| Entity | Tekil isim, sonek yok | `Survey`, `Question` |
| DTO | `<Kaynak><İşlem>Request` / `<Kaynak>Response` | `SurveyCreateRequest`, `SurveyDetailResponse` |

**Hata yönetimi**
- Service'ten `ApiException` (veya `NotFoundException`, `BusinessRuleException`) fırlatın; `code` değeri
  OpenAPI spec'teki hata koduyla aynı olmalı (`SURVEY_HAS_RESPONSES` gibi).
- Tüm hatalar RFC 7807 `application/problem+json` döner: `type, title, status, code, errors[]`.

**Güvenlik**
- Roller: `ADMIN` (`/admin/**`), `USER` (`/user/**`). JWT'de `roles` claim'i içinde taşınır.
- Token üretimi için `JwtTokenService.issueAccessToken(...)` kullanılır. Access token 15 dk.
- Refresh token tek kullanımlıktır (rotation). Oturum son refresh'ten 30 dk sonra veya en geç 7 günde düşer.
- Şifreler `PasswordEncoder` (BCrypt) bean'i ile hash'lenir.
- Ek yetki kuralı gerekiyorsa `@PreAuthorize("hasRole('ADMIN')")` kullanın.

## Dev seed kullanıcıları

`dev` profilinde uygulama her açılışta `src/main/resources/db/seed/dev-users.sql` dosyasını çalıştırır
(var olan e-postaları atlar). Şifreler BCrypt ile hash'lenmiştir; aşağıdaki değerler yalnızca yerel geliştirme içindir.

| E-posta | Rol | Şifre |
|---|---|---|
| `ayse.yilmaz@definex.com` | ADMIN | `Admin123!` |
| `mehmet.demir@definex.com` | ADMIN | `Admin123!` |
| `zeynep.kaya@definex.com` | USER | `User123!` |
| `can.ozturk@definex.com` | USER | `User123!` |
| `elif.sahin@definex.com` | USER | `User123!` |

`USER` rolündeki hesaplar admin girişinde `401 INVALID_CREDENTIALS` alır.

## Örnek istekler (Admin auth)

```bash
# 1. Login → accessToken + refreshToken
curl -s -X POST http://localhost:8080/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ayse.yilmaz@definex.com","password":"Admin123!"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshToken": "q7Zx0bR3kN9vYpL2mT6sWc1eHfJ4uAaD8gKiOn5rXyQ",
  "refreshExpiresIn": 1800,
  "user": { "id": "0a1b2c3d-0000-4000-8000-000000000001", "fullName": "Ayşe Yılmaz",
            "email": "ayse.yilmaz@definex.com", "role": "ADMIN" }
}
```

```bash
# 2. Korumalı uç (access token ile)
curl -i http://localhost:8080/api/v1/admin/surveys -H "Authorization: Bearer <accessToken>"

# 3. Refresh → yeni token çifti (eski refresh token artık geçersiz; tekrar kullanılırsa oturum iptal edilir)
curl -s -X POST http://localhost:8080/api/v1/admin/auth/refresh \
  -H "Content-Type: application/json" -d '{"refreshToken":"<refreshToken>"}'

# 4. Logout → 204
curl -i -X POST http://localhost:8080/api/v1/admin/auth/logout -H "Authorization: Bearer <accessToken>"
```

## Örnek istekler (Admin anketler)

Anket yapısı bölümsüzdür: anket → sorular → seçenekler. Oluşturma ve güncelleme tek uçtan yapılır: **`POST /admin/upsertSurvey`**.
gövdede `id` yoksa yeni taslak oluşur (`201`), varsa güncellenir (`200`). Sorular ve seçenekler için de `id`'li olan
güncellenir, `id`'siz olan eklenir, listede olmayan silinir; dizi sırası = gösterim sırası. Kaydetmede yalnızca
`title` zorunludur.

```bash
# 1. İlk kayıt (id yok) → 201; yanıtta anket, soru ve seçenek id'leri döner
curl -s -X POST http://localhost:8080/api/v1/admin/upsertSurvey \
  -H "Authorization: Bearer <accessToken>" -H "Content-Type: application/json" \
  -d '{"title":"2026 Q4 Çalışan Memnuniyeti","responseMode":"ANONYMOUS",
       "questions":[{"text":"Şirketimizde çalışmaktan memnunum.","type":"LIKERT","required":true},
                    {"text":"Hangi yan hakları kullanıyorsunuz?","type":"MULTIPLE_CHOICE",
                     "options":[{"label":"Özel sağlık sigortası"}]}]}'

# 2. Sonraki kayıt (id'ler geri gönderilir) → 200
#    Likert sorusu listede olmadığı için silinir, yeni seçenek eklenir.
curl -s -X POST http://localhost:8080/api/v1/admin/upsertSurvey \
  -H "Authorization: Bearer <accessToken>" -H "Content-Type: application/json" \
  -d '{"id":"<surveyId>","title":"2026 Q4 Çalışan Memnuniyeti","responseMode":"ANONYMOUS",
       "endsAt":"2026-11-30T21:00:00Z",
       "questions":[{"id":"<questionId>","text":"Hangi yan hakları kullanıyorsunuz?","type":"MULTIPLE_CHOICE",
                     "options":[{"id":"<optionId>","label":"Özel sağlık sigortası"},{"label":"Yemek kartı"}]}]}'

# 3. Liste, detay, silme (yalnızca DRAFT)
curl -s "http://localhost:8080/api/v1/admin/surveys?status=DRAFT&sort=-updatedAt&page=1&size=20" \
  -H "Authorization: Bearer <accessToken>"
curl -s http://localhost:8080/api/v1/admin/surveys/<surveyId> -H "Authorization: Bearer <accessToken>"
curl -i -X DELETE http://localhost:8080/api/v1/admin/surveys/<surveyId> -H "Authorization: Bearer <accessToken>"
```

| Durum | HTTP | `code` |
|---|---|---|
| Anket yok veya başka admin'e ait | 404 | `SURVEY_NOT_FOUND` |
| `settings` tipe uymuyor / bilinmeyen alan | 400 | `VALIDATION_ERROR` |
| Tekrarlanan veya bu ankete ait olmayan id; seçimli olmayan tipe seçenek | 422 | `INVALID_STRUCTURE` |
| Yayındaki ankette metin dışı değişiklik | 409 | `SURVEY_STRUCTURE_LOCKED` |
| Süresi dolmuş / kapalı / arşivli anketi kaydetme | 409 | `SURVEY_NOT_EDITABLE` |
| Taslak olmayan anketi silme | 409 | `SURVEY_NOT_DELETABLE` |

## Örnek istekler (Yayınlama)

Katılımcılar `users` tablosundaki `USER` rolündeki aktif kullanıcılardır. Admin anket düzenleme ekranında
kullanıcıları listeleyip seçer, ardından anketi yayınlar. Her katılımcı için davete özel bir link üretilir
(`{FRONTEND_BASE_URL}/s/{token}`); token'ın yalnızca hash'i saklanır. Link yalnızca davet mailiyle gider; yanıtta dönmez
(dev profilinde log'a yazılır).

```bash
# 1. Davet edilebilecek kullanıcılar (ad/e-posta araması, sayfalı)
curl -s "http://localhost:8080/api/v1/admin/getAllUsers?q=zeynep&page=1&size=20" \
  -H "Authorization: Bearer <accessToken>"

# 2. Yayınla → 200; anket PUBLISHED olur, davet mailleri kuyruğa alınır (emailStatus: QUEUED)
curl -s -X POST http://localhost:8080/api/v1/admin/publishSurvey \
  -H "Authorization: Bearer <accessToken>" -H "Content-Type: application/json" \
  -d '{"surveyId":"<surveyId>","userIds":["0a1b2c3d-0000-4000-8000-000000000101"]}'
```

Yayınlamadan önce tamlık kontrol edilir; eksikler tek seferde döner:

| Durum | HTTP | `code` |
|---|---|---|
| Bitiş tarihi yok / geçmiş, yanıt modu yok, soru yok, soru metni/tipi boş, seçimli soruda 2'den az seçenek | 422 | `SURVEY_INCOMPLETE` |
| Kullanıcı bulunamadı, `USER` değil veya pasif | 422 | `INVALID_PARTICIPANTS` |
| Anket taslak değil (tekrar yayınlama dahil) | 409 | `INVALID_SURVEY_STATUS` |

Yayındaki anketin bitiş tarihi geçince durum en geç bir dakika içinde otomatik olarak `EXPIRED` olur.

## Davet mailleri

Mailler `JavaMailSender` ile SMTP üzerinden gönderilir. Publish, her davet için `email_messages` tablosuna bir kayıt
yazar (aynı transaction). Commit sonrası mail arka planda **bir kez** gönderilir ve `SENT` ya da `FAILED` olur.
Otomatik yeniden deneme yoktur: admin ekrandan durumlara göre filtreler ve gönderilemeyenleri seçip yeniden gönderir.

```bash
# Gönderilemeyen mailler (filtreler opsiyonel ve birlikte kullanılabilir)
curl -s "http://localhost:8080/api/v1/admin/getMails?surveyId=<surveyId>&emailStatus=FAILED" \
  -H "Authorization: Bearer <accessToken>"

# Seçilenleri yeniden gönder (davet linki yenilenir; eski link geçersiz olur)
curl -s -X POST http://localhost:8080/api/v1/admin/resendMails \
  -H "Authorization: Bearer <accessToken>" -H "Content-Type: application/json" \
  -d '{"emailIds":["<emailId>"]}'
```

| Filtre | Değerler |
|---|---|
| `emailStatus` | `QUEUED`, `SENDING`, `SENT`, `FAILED` (çoklu) |
| `invitationStatus` | `PENDING`, `STARTED`, `COMPLETED`, `CANCELLED` (çoklu) |
| `surveyId`, `q`, `page`, `size` | anket, alıcı adı/e-posta araması, sayfalama |

Yanıttaki `summary` durum başına sayıları, her satırdaki `resendable` yeniden gönderilebilirliği verir.
10 dakikadan uzun `SENDING` durumunda kalan mail `stuck: true` olarak işaretlenir ve yeniden gönderilebilir.
Uygun olmayan bir mail seçilirse hiçbiri gönderilmez (`422 MAILS_NOT_RESENDABLE`).

### Gmail ile gerçek gönderim (dev)

1. Gmail hesabında 2 adımlı doğrulamayı açın ve https://myaccount.google.com/apppasswords adresinden bir
   uygulama şifresi oluşturun.
2. `.env` dosyasına yazın (boşluksuz): `MAIL_USERNAME=hesap@gmail.com`, `MAIL_PASSWORD=<16 haneli şifre>`.
3. `docker compose up --build`. Gönderim sonucu `docker logs survey-api` içinde (`Mail gönderildi` /
   `Mail gönderilemedi`) ve `getMails` ucunda görünür.

### Gerçek katılımcılar (yerel seed)

`dev-users.sql` içindeki katılımcıların adresleri sahte olduğundan onlara mail gitmez veya yanlış kişiye gidebilir.
Gerçek kişileri **git'e eklenmeyen** `src/main/resources/db/seed/local-users.sql` dosyasına ekleyin
(şablon: `local-users.sql.example`). Dev profilinde dosya varsa her açılışta çalıştırılır.

## Hata yanıtı örnekleri

```bash
# Token olmadan korumalı uç → 401
curl -i http://localhost:8080/api/v1/admin/surveys
```

```json
{
  "type": "https://survey.example.com/problems/unauthorized",
  "title": "Oturum geçersiz",
  "status": 401,
  "instance": "/api/v1/admin/surveys",
  "code": "UNAUTHORIZED"
}
```

| Durum | HTTP | `code` |
|---|---|---|
| E-posta/şifre hatalı veya hesap admin değil | 401 | `INVALID_CREDENTIALS` |
| Çok sayıda hatalı deneme (`Retry-After` başlığıyla) | 423 | `ACCOUNT_LOCKED` |
| Refresh token geçersiz, kullanılmış, oturum boşta kaldı veya süresi doldu | 401 | `SESSION_EXPIRED` |
| Alan doğrulaması (`errors[]` alan bazında) | 400 | `VALIDATION_ERROR` |
