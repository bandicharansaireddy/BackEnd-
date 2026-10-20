# FreshMart API

Spring Boot 3 / Java 17 API for the separate FreshMart frontend repository. The backend Git remote is `https://github.com/bandicharansaireddy/BackEnd-.git`.

## Current implementation

- Maven project with Spring Web, Security, Data JPA, Bean Validation, MySQL, Flyway, JJWT, and test dependencies
- Flyway `V1` creates categories and products and seeds a small illustrative catalog
- Public product/category read APIs with paging, search, category, price filtering, and sorting
- DTO responses, CORS origin configuration, and Hibernate schema validation
- Registration, email/mobile login, BCrypt password hashes, `/me`, short-lived issuer-checked JWT access tokens, and rotating hashed refresh sessions in HttpOnly cookies
- MySQL credentials and JWT signing secret are read from the environment

Password reset OTP, change password, persistent carts, addresses, orders, admin APIs, and complete session invalidation remain future milestones. Do not treat this API as production-ready until those flows and their tests are implemented.

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

Alternatively, copy `application-local.example.yml` to ignored `application-local.yml`, supply values via environment variables, and activate `local` with `SPRING_PROFILES_ACTIVE=local`. Never place real credentials in tracked YAML.

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
```

See `docs/freshmart-api.postman_collection.json` for an importable Postman collection. Product responses use a stable `content`, `number`, `size`, and totals page envelope. Prices are server-owned INR values; clients must not submit totals for order calculation.

## Security status

Refresh cookies are HttpOnly, SameSite=Strict, scoped to `/api/auth`, and use the configurable Secure flag (`app.jwt.refresh-secure`, enable it for HTTPS deployments). Refresh and logout require the exact configured frontend `Origin`. Access tokens are returned in JSON and should remain in frontend memory; the refresh token is never returned to JavaScript. Logout revokes the supplied refresh session; access JWTs expire after a short configured lifetime. Public catalog/auth entry points are open; other routes require a valid signed JWT. Password reset OTP delivery, rate limits, password change, invalidating all sessions after reset, admin roles, and broader authorization tests remain unfinished. No payment-card data is collected.

## Build and tests

```powershell
mvn test
mvn package
```

These commands require Maven and a reachable dependency repository. Integration tests will also need the configured MySQL database or a dedicated test database.
