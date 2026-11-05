# AGENTS.md

## What this is

Single-module Spring Boot 4.1.2 / Java 25 REST app for a JPA-relations exercise (futbol domain: Club, Jugador, Entrenador, Asociacion, Competicion). Code lives under `src/main/java/com/example/demo/app`.

## Commands (Windows, use the wrapper)

- Compile check: `.\mvnw.cmd -q compile`
- Run app: `.\mvnw.cmd spring-boot:run` (port **8082**)
- Run tests: `.\mvnw.cmd test` — only test is `@SpringBootTest contextLoads`.

## Database

H2 **file** database at `jdbc:h2:file:./data/relacionesdb` (no external server; the `data/` dir with `relacionesdb.mv.db` is created on startup and is git-ignored). Web console at `/h2-console` (user `sa`, empty password). App runs on port **8082**.

## Architecture & conventions

- Package and class names are in **Spanish** (`controlador`, `entidades`, `repositorio`, `dto`) — keep this.
- Flow: `controlador` (@RestController + @Transactional; uses Spring Data JPA repositories directly) → `repositorio` → `entidades`.
- **No intermediate logic layer exists, on purpose (instructor's decision).** The logic (validation, DTO assembly, transactions) lives in the controllers; do not create any class between them and the repositories.
- **Never return entities from the API.** All relationships are LAZY; controllers build DTOs inside `@Transactional` methods to avoid `LazyInitializationException` and infinite JSON recursion. Response DTOs convert entities via their own static method (`ClubDTO.desde(club)`). `Club` owns all 4 relationships (unidirectional).
- DELETE endpoints call `repository.delete(...)` + `repository.flush()` inside the transactional method so FK violations (`DataIntegrityViolationException` → 409) happen inside the method, not at commit, and reach the `ApiExceptionHandler` advice.
- Status codes: 200 GET/PUT, 201 POST, 204 DELETE, **404** unknown id/reference, **409** FK violation (handled in advice, not per-controller).

## Schema / DB quirks

- `ddl-auto=update` does **not** modify an existing FK. The `ON DELETE CASCADE` on `jugadores.id_club` (`@OnDelete`) only lands if the DB is recreated with `ddl-auto=create` or `create-drop` on a clean schema.
- `DatosPruebaRunner` (config) seeds the DB on startup, but only when the `clubes` table is empty.
