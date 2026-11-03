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
| `SERVER_PORT` | | `8080` | HTTP portu |
| `DB_NAME`, `DB_PORT` | | `survey`, `5432` | Yalnızca docker-compose için |

Gizli bilgiler koda veya `application*.yml` dosyalarına yazılmaz. `.env` git'e eklenmez.

## Proje yapısı

```
com.definex.survey
├─ config/      @ConfigurationProperties (JWT, CORS), CORS bean'i
├─ security/    SecurityFilterChain, JWT encoder/decoder, JwtTokenService, Roles, 401/403 handler'ları
├─ exception/   GlobalExceptionHandler, ProblemDetailFactory, ApiException ve alt sınıfları
├─ common/      modüllerden bağımsız yardımcılar
├─ auth/        /admin/auth/*, /user/auth/*
├─ survey/      /admin/surveys/*
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
- Token üretimi için `JwtTokenService.issueAccessToken(...)` kullanılır. Access token 15 dk, refresh token 7 gün.
- Şifreler `PasswordEncoder` (BCrypt) bean'i ile hash'lenir.
- Ek yetki kuralı gerekiyorsa `@PreAuthorize("hasRole('ADMIN')")` kullanın.

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

Login → korumalı uç akışının `curl` örnekleri, auth uçları yazıldığında bu bölüme eklenecek.
