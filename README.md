# FreshMart API

Spring Boot 3 / Java 17 API for the separate FreshMart frontend repository. The backend Git remote is `https://github.com/bandicharansaireddy/BackEnd-.git`.

## Current implementation

- Maven project with Spring Web, Security, Data JPA, Bean Validation, MySQL, Flyway, JJWT, and test dependencies
- Flyway `V1` creates categories and products and seeds a small illustrative catalog
- Public product/category read APIs with paging, search, category, price filtering, and sorting
- DTO responses, CORS origin configuration, and Hibernate schema validation
- MySQL credentials and JWT signing secret are read from the environment

Authentication, refresh-token rotation, accounts, cart, addresses, orders, and admin APIs remain future milestones. Do not treat this API as production-ready until those flows and their tests are implemented.

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
$env:JWT_SECRET = '<a-random-base64-secret-of-at-least-32-bytes>'
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
```

See `docs/freshmart-api.postman_collection.json` for an importable Postman collection. Product responses use a stable `content`, `number`, `size`, and totals page envelope. Prices are server-owned INR values; clients must not submit totals for order calculation.

## Security status

JWT dependencies and environment properties are present as architecture groundwork. Authentication endpoints and JWT validation are not implemented yet. Public catalog reads are open; all other request paths require authentication, and no payment-card data is collected. Cookie refresh transport, CSRF strategy, OTP delivery, abuse limits, session revocation, and authorization tests are required before real use.

## Build and tests

```powershell
mvn test
mvn package
```

These commands require Maven and a reachable dependency repository. Integration tests will also need the configured MySQL database or a dedicated test database.
