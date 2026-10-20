# FreshMart API

Spring Boot 3 / Java 17 API for the separate FreshMart frontend repository. The backend Git remote is `https://github.com/bandicharansaireddy/BackEnd-.git`.

## Current implementation

- Maven project with Spring Web, Security, Data JPA, Bean Validation, MySQL, Flyway, JJWT, and test dependencies
- Flyway `V1` creates categories and products and seeds a small illustrative catalog
- Public product/category read APIs with paging, search, category, price filtering, and sorting
- DTO responses, CORS origin configuration, and Hibernate schema validation
- Registration, email/mobile login, BCrypt password hashes, `/me`, short-lived issuer-checked JWT access tokens, and rotating hashed refresh sessions in HttpOnly cookies
- Forgot-password OTP creation/verification, attempt and expiry checks, password change/reset, and refresh-session revocation
- Owner-scoped persistent carts and delivery addresses, plus transactional order creation with product locks, server-calculated totals, and stock updates
- MySQL credentials and JWT signing secret are read from the environment

Admin product/category/order management, user administration, a production mail/SMS provider, and full database integration testing remain future milestones. Orders are recorded as `DEMO_UNPAID`; this project does not process payment.

## Prerequisites

- Java 17 or newer (Java 27 is also suitable)
- Maven 3.9+
- MySQL Server 8.0.46

Maven was not available on PATH during initial project inspection. Install Maven, then verify with `mvn -version` before building.

## Database setup

Start the Windows service `MySQL80`, then create the local database and a least-privileged development account in MySQL Workbench or the MySQL CLI:

```sql
CREATE DATABASE freshmart CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'freshmart'@'localhost' IDENTIFIED BY 'choose-a-local-password';
GRANT ALL PRIVILEGES ON freshmart.* TO 'freshmart'@'localhost';
```

Replace the sample password with a local value and keep it out of source control. The API runs Flyway migrations on startup; Hibernate uses `ddl-auto: validate` and does not create production schema.

## Configure and run

Set local environment variables in PowerShell (values are examples; do not commit them):

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/freshmart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME = 'freshmart'
$env:DB_PASSWORD = '<your-local-database-password>'
$env:FRONTEND_ORIGIN = 'http://localhost:5173'
$keyBytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($keyBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($keyBytes)
[Array]::Clear($keyBytes, 0, $keyBytes.Length)
mvn spring-boot:run
```

Alternatively, copy `application-local.example.yml` to ignored `application-local.yml`, supply values via environment variables, and activate `local` with `SPRING_PROFILES_ACTIVE=local`. The local profile stores OTP messages in memory and exposes them once at `GET http://localhost:8080/api/dev/otp?email=buyer@example.com`; the endpoint accepts loopback requests only. OTPs expire after five minutes and are never logged. Outside the `local` profile, the no-op delivery adapter does not send mail; configure a real provider before deploying password recovery. Never place real credentials in tracked YAML.

Startup order: start MySQL, start the backend at `http://localhost:8080`, then start the frontend at `http://localhost:5173`.

## API examples

```http
GET http://localhost:8080/api/products?q=mango&category=fruit&page=0&size=12&sort=price-asc
GET http://localhost:8080/api/products/1
GET http://localhost:8080/api/categories
GET http://localhost:8080/api/categories/1/products?page=0&size=12
POST http://localhost:8080/api/auth/register
POST http://localhost:8080/api/auth/login
POST http://localhost:8080/api/auth/refresh
POST http://localhost:8080/api/auth/logout
GET http://localhost:8080/api/auth/me  (Authorization: Bearer <short-lived access token>)
POST http://localhost:8080/api/auth/forgot-password
POST http://localhost:8080/api/auth/verify-otp
POST http://localhost:8080/api/auth/reset-password
PUT  http://localhost:8080/api/auth/change-password  (Authorization: Bearer <short-lived access token>)
GET  http://localhost:8080/api/cart                 (Authorization: Bearer <short-lived access token>)
POST http://localhost:8080/api/cart/items           (Authorization: Bearer <short-lived access token>)
PUT  http://localhost:8080/api/cart/items/{id}      (Authorization: Bearer <short-lived access token>)
DELETE http://localhost:8080/api/cart/items/{id}    (Authorization: Bearer <short-lived access token>)
GET  http://localhost:8080/api/addresses            (Authorization: Bearer <short-lived access token>)
POST http://localhost:8080/api/addresses            (Authorization: Bearer <short-lived access token>)
POST http://localhost:8080/api/orders               (Authorization: Bearer <short-lived access token>)
GET  http://localhost:8080/api/orders               (Authorization: Bearer <short-lived access token>)
GET  http://localhost:8080/api/orders/{id}          (Authorization: Bearer <short-lived access token>)
```

Cart item request: `{"productId":1,"quantity":2}`. Address request: `{"fullName":"Fresh Buyer","mobile":"9876543210","addressLine":"12 Market Road, Jubilee Hills","city":"Hyderabad","pincode":"500033"}`. Place an order with `{"addressId":1}`; the API reads the authenticated user's cart, checks stock under product row locks, computes subtotal and delivery fee, decrements inventory, clears the cart, and returns an order marked `DEMO_UNPAID`. Clients must not submit prices or totals.

See `docs/freshmart-api.postman_collection.json` for an importable Postman collection. Product and order lists use a stable `content`, `number`, `size`, and totals page envelope.

## Security status

Refresh cookies are HttpOnly, SameSite=Strict, scoped to `/api/auth`, and use the configurable Secure flag (`JWT_REFRESH_SECURE=true` for HTTPS deployments). Refresh and logout require the exact configured frontend `Origin`. Access tokens are returned in JSON and should remain in frontend memory; the refresh token is never returned to JavaScript. Logout revokes the supplied refresh session; password changes/resets revoke all refresh sessions; access JWTs expire after a short configured lifetime. Reset codes are BCrypt-hashed in MySQL, expire after five minutes, allow five verification attempts, and can be requested once per account per minute. Forgot-password requests are limited to ten per source IP per fifteen-minute window using an HMAC of the address (no raw IP is stored); put the API behind a trusted proxy and configure its network limits as well. The reset challenge is a one-use, five-minute JWT bound to the verified OTP row. Public catalog/auth entry points are open; other routes require a valid signed access JWT. Admin roles and broader ownership tests remain unfinished. No payment-card data is collected.

## Build and tests

```powershell
mvn test
mvn package
```

These commands require Maven and a reachable dependency repository. Integration tests will also need the configured MySQL database or a dedicated test database.
