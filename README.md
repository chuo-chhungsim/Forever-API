# Forever API

Spring Boot 4 (Java 21) REST API for the Forever fashion store. Serves the React web app and the Flutter app (`../forever_app`).

| | |
|---|---|
| Swagger UI | http://localhost:8989/swagger-ui.html (`/` redirects here). Two definitions in the **Select a definition** dropdown: **User API** (store + mobile app) and **Admin API** (`/api/admin/**`) |
| OpenAPI JSON | http://localhost:8989/v3/api-docs |
| Database | PostgreSQL, database `forever` (Flyway migrations in `src/main/resources/db/migration`) |
| Images | MinIO bucket `forever` (streamed to clients through `/api/media/**`) |
| OTP e-mails | SMTP; in dev they land in Mailpit at http://localhost:8025 |

## Run

```bash
docker compose up -d      # Mailpit only; Postgres (5432) and MinIO (9000) are expected to be running already
./mvnw spring-boot:run
```

Configuration is read from `application.yml` and an untracked `./.env.local` (keys below). On first start the app
creates the MinIO bucket, seeds the admin user and imports the 52 products + 55 images from the web app.

| Key | Meaning |
|---|---|
| `DB_PASSWORD` | password of the `forever` Postgres role (`DB_URL`, `DB_USERNAME` override the rest) |
| `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` | MinIO credentials (`MINIO_ENDPOINT`, `MINIO_BUCKET` optional) |
| `JWT_SECRET` | base64 HMAC secret, at least 32 bytes |
| `OTP_LOG_CODE` | `true` prints OTP codes in the log. **Dev only** |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP server (defaults: Mailpit on localhost:1025) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | seeded admin (default `admin@forever.com` / `Admin@12345`, change it) |

## Project structure

Module-based: shared infrastructure at the top, one folder per business module under `modules/`.

```
com.forever.foreverapi
├── advice/          global exception handler
├── base/            shared payloads (ErrorResponse, PageResponse, AuthUser ...)
├── client/          adapters to external systems (minio/, mail/)
├── component/       shared beans (JwtService, MediaUrls, DataSeeder)
├── configuration/   Spring config (security, OpenAPI, CORS, typed properties)
├── exception/       ApiException
└── modules/
    ├── auth/        controller · dto · job · model · repository · service
    ├── user/        controller · dto · model · repository · service
    ├── product/     controller · dto · mapper · model · repository · service
    ├── cart/        controller · dto · model · repository · service
    ├── order/       controller · dto · mapper · model · repository · service
    ├── admin/       controller (product + order management)
    └── media/       controller (image streaming from MinIO)
```

## API overview

Everything is documented and callable in Swagger UI (use **Authorize** with the `accessToken`).

* **Auth** `/api/auth` – `register` -> e-mails an OTP -> `verify-email` (returns tokens); `login`; `refresh` (rotating refresh tokens);
  `logout`; `resend-otp`; `forgot-password` -> `reset-password` (OTP + new password). OTPs are 6 digits, hashed at rest,
  expire after 10 min, allow 5 attempts and a 60 s resend cooldown. Unverified logins get `403 EMAIL_NOT_VERIFIED`. Password guessing is throttled per e-mail: 5 wrong passwords in 15 min lock login for 15 min (`429 TOO_MANY_LOGIN_ATTEMPTS`; a password reset clears it). The counter is in memory, so use a shared store (e.g. Redis) if you run several instances.
* **Profile** `/api/users/me` – read/update profile, `change-password`.
* **Products** `/api/products` (public) – search/filter/sort/paginate, `filters`, `latest`, `bestsellers`, `{id}`, `{id}/related`.
* **Cart** `/api/cart` – server-side cart with totals (delivery fee 10).
* **Orders** `/api/orders` – place from cart, list, get, cancel (while `ORDER_PLACED`/`PACKING`).
* **Admin** `/api/admin/**` (role `ADMIN`, shown as its own *Admin API* group in Swagger) – dashboard stats, product CRUD with image upload to MinIO (including hidden products), order list and status updates. Used by the web admin panel.

Errors share one JSON shape: `{timestamp,status,code,message,fieldErrors?,path}`.

## Notes

* Only **cash on delivery** can be ordered. Stripe/Razorpay appear in the web UI but are not integrated; the API answers `400 PAYMENT_METHOD_UNSUPPORTED`.
* Product images are returned as absolute URLs built from the host the client called, so emulators and phones get a reachable address.
* Deleting a product in the admin API only hides it, so existing orders keep working.
* `mvn test` needs Postgres and MinIO running (it boots the full context).
