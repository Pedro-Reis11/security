# AGENTS: How to work with this codebase

Short checklist for an AI coding agent working in this repository:
- Understand the runtime shape: Spring Boot REST app + JPA + OAuth2 resource-server JWT auth.
- Key files: `pom.xml`, `src/main/resources/application.yaml`, `data.sql`, `docker/docker-compose.yaml`.
- Primary Java modules: `config` (SecurityConfig, AdminUserConfig), `controller` (TokenController), `entity` (User, Role, Tweet), `repository` (UserRepository, RoleRepository).
- Build/run locally before changing code (see commands below).

Quick dev commands (Windows PowerShell):
- Start DB: `docker compose -f docker\docker-compose.yaml up -d` (maps MySQL container port 3306 -> host 3307)
- Build: `.\mvnw.cmd -DskipTests clean package` or run: `.\mvnw.cmd spring-boot:run`
- Run tests: `.\mvnw.cmd test`

What the app does (big picture):
- This is a tiny tweet-style service that stores Users, Roles and Tweets in MySQL via Spring Data JPA.
- Security: app is configured as an OAuth2 Resource Server using JWTs. `SecurityConfig`:
  - Permits POST `/login` to issue tokens.
  - Requires a Bearer JWT for all other endpoints.
  - JWT encoding/decoding uses Nimbus and RSA key files located in `src/main/resources/app.key` and `app.pub` (referenced in `application.yaml` via `jwt.private.key` / `jwt.public.key`).
- Token issuance: `TokenController#login` validates credentials, issues a short-lived JWT (expires in 300s) using the project's `JwtEncoder` bean.

Project-specific patterns and notable discoveries (use these to avoid wasted work):
- DTOs use Java records (e.g. `LoginRequest`, `LoginResponse`). Follow that style for small, immutable request/response shapes.
- Password encoding: `BCryptPasswordEncoder` is provided as a bean in `SecurityConfig` but `TokenController` instantiates its own instance instead of injecting the bean. Prefer injecting the bean for consistency.
- Entity patterns:
  - `User.id` is a UUID (`GenerationType.UUID`).
  - `User.roles` is a ManyToMany with EAGER fetch and `CascadeType.ALL` (watch for accidental cascading deletes/updates and serialization recursion).
  - `Tweet` has a ManyToOne relation to `User` and `creationTimestamp` annotated with `@CreationTimestamp`.
- Role constants: `Role.Values` enum defines BASIC(1) and ADMIN(2) (note the values). However `src/main/resources/data.sql` inserts (1,'admin') and (2,'user') — this is a discoverable mismatch (case + id mapping) that will cause `RoleRepository.findByName(Role.Values.ADMIN.name())` to return null. Agents should not blindly assume the DB seed matches the enum.
- Case-sensitivity: `RoleRepository.findByName` expects the exact `name` string; data.sql stores lowercase role names ('admin','user') while code uses `Role.Values.ADMIN.name()` which yields `"ADMIN"`. Either update data.sql or normalize/case-insensitive lookup.
- Incomplete/placeholder code: `UserController` source is an unfinished stub (see `src/main/java/.../UserController.java`). Expect compiled artifacts to differ from source. Validate edits by building.

Integration points / external dependencies:
- MySQL (configured in `application.yaml` to connect to localhost:3307/mydb with user `admin` / `123`). Use the provided docker-compose to create this DB locally.
- RSA keys: `src/main/resources/app.key` and `app.pub`. If you replace these keys, keep PEM format and update `application.yaml` or the properties accordingly; `SecurityConfig` expects to bind them to `RSAPublicKey`/`RSAPrivateKey`.
- Spring Boot / Spring Security / Spring Data JPA / Nimbus JWT (see `pom.xml` for exact dependencies).

Testing & debugging tips specific to this project:
- To exercise authentication quickly:
  1) Ensure DB is up and seeded: `docker compose -f docker\docker-compose.yaml up -d` then run the app.
  2) Login: POST JSON {"username":"admin","password":"123"} to `http://localhost:8080/login` (Content-Type: application/json). The app expects an `admin` user created by `AdminUserConfig` on startup — but beware the Role lookup mismatch noted above.
  3) Use the returned token: add header `Authorization: Bearer <token>` to protect requests.
- If JWT conversion/binding fails at startup, check the RSA files are readable and in PEM format; `SecurityConfig` builds a JWK from the keys.
- To quickly find relevant code paths when changing auth behavior: follow `SecurityConfig` -> `JwtEncoder` bean -> `TokenController` (issues tokens) -> `UserRepository`/`User.isLoginCorrect` (password check).

Safe-first edit checklist for agents making changes:
- Run `.\mvnw.cmd -DskipTests clean package` after edits to catch compile or bean-binding errors.
- If changes touch DB seed or Role enum, run the app against the dockerized MySQL to reproduce startup behavior.
- Prefer injecting beans (e.g., `BCryptPasswordEncoder`) rather than creating new instances inline.

References (key files to open first):
- `src/main/java/pedrodev/springsecurity/config/SecurityConfig.java`
- `src/main/java/pedrodev/springsecurity/config/AdminUserConfig.java`
- `src/main/java/pedrodev/springsecurity/controller/TokenController.java`
- `src/main/java/pedrodev/springsecurity/entity/*`
- `src/main/resources/application.yaml`, `src/main/resources/data.sql`, `docker/docker-compose.yaml`

If you modify startup seeding, role enums or JWT keys, run a full build and start the dockerized DB to validate behavior end-to-end.

