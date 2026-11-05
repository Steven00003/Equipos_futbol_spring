# relaciones_spring

Práctica de **relaciones JPA** con Spring Boot 4.1.2, Java 25 e **H2 en archivo**
(`jdbc:h2:file:./data/relacionesdb`), sin servidor externo (sin XAMPP ni MySQL).
La aplicación corre en el **puerto 8082**.

## Base de datos (H2 en archivo)

- Se guarda en `./data/relacionesdb.mv.db`; se crea sola al arrancar (si la carpeta `data`
  no existe, H2 la crea).
- **Consola H2**: <http://localhost:8082/h2-console> — JDBC URL: `jdbc:h2:file:./data/relacionesdb`,
  usuario `sa`, contraseña vacía.
- Para volver a la BD limpia (p. ej. para que reaparezca la FK con `ON DELETE CASCADE`, ver más abajo),
  basta con borrar el archivo `data/relacionesdb.mv.db` con la aplicación parada.

## Ejecución

- Desde Eclipse (Spring Tools), o `.\mvnw.cmd spring-boot:run`
- Verificar compilación: `.\mvnw.cmd -q compile`
- La FK con `ON DELETE CASCADE` en `jugadores.id_club` solo queda en la tabla real si la FK se
  vuelve a crear (por ejemplo, con la BD limpia y `ddl-auto=create-drop`/`create`; con
  `ddl-auto=update` Hibernate no modifica la FK ya existente).

## Las 4 relaciones

Todas son unidireccionales con **`Club` como dueño** y **LAZY**.

| Anotación | Clases | Qué genera en la BD |
|---|---|---|
| `@OneToOne(fetch = LAZY)` + `@JoinColumn(name = "id_entrenador", unique = true)` | `Club` → `Entrenador` | FK `id_entrenador` (UNIQUE) en la tabla `clubes`, sin tabla intermedia |
| `@OneToMany(fetch = LAZY, cascade = ALL)` + `@JoinColumn(name = "id_club")` + `@OnDelete(CASCADE)` | `Club` → `Jugador` | FK `id_club` en la tabla `jugadores`, sin tabla intermedia; `ON DELETE CASCADE` en esa FK |
| `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "id_asociacion")` | `Club` → `Asociacion` | FK `id_asociacion` en la tabla `clubes` |
| `@ManyToMany(fetch = LAZY)` + `@JoinTable(name = "clubes_competiciones", ...)` | `Club` ↔ `Competicion` | Tabla intermedia `clubes_competiciones` (`id_club`, `id_competicion`) |

## LAZY frente a EAGER

- **EAGER**: la relación se carga automáticamente en **cada** consulta, aunque no se necesite; encadenadas, disparan más JOINs o N+1.
- **LAZY**: solo se carga cuando se navega el atributo, devolviendo un proxy.
- Se usó **LAZY** porque las entidades NO se devuelven por la API: el controlador construye DTOs
  dentro de métodos `@Transactional`, cuando la sesión sigue abierta y navegar el LAZY es
  seguro. Así se evitan tanto la `LazyInitializationException` como el JSON infinito de
  relaciones ciclicas (StackOverflowError) que producen los proxies LAZY si se serializa la entidad directamente.

## @OnDelete(CASCADE) y la FK por defecto

- **Restricción por defecto**: al crear una FK, Hibernate la deja con `ON DELETE RESTRICT`
  (o `NO ACTION`): si otro registro referencia la fila, el motor rechaza el borrado con un
  error de clave foránea (la API lo convierte en **409**).
- **`@OnDelete(action = OnDeleteAction.CASCADE)`** (de Hibernate, no de JPA) delega el borrado
  al **motor de BD**: pone `ON DELETE CASCADE` en la FK `jugadores.id_club`, de modo que
  `DELETE /clubes/{id}` borra los jugadores del club directamente con un `DELETE` del club,
  sin ejecutar un DELETE por cada jugador desde la aplicación.

## API REST (puerto 8082)

Estructura: **Controlador → Repositorio (Spring Data JPA) → Base de datos**.

- `controlador` (`@RestController` + `@RestControllerAdvice`): construye los DTOs, valida las
  referencias (404) y aplica la lógica. Los métodos que navegan relaciones LAZY o escriben en la
  BD llevan `@Transactional`; los DELETE usan `delete()` + `flush()` para que una violación de FK
  ocurra dentro del método y llegue al `@RestControllerAdvice` como **409**.
- `dto`: cada DTO de respuesta convierte su entidad con un método estático propio
  (p. ej. `ClubDTO.desde(Club)`).
- No hay capa de lógica intermedia: Spring Data JPA (`repositorio`) ya provee el acceso a datos
  (`findById`, `findAll`, `save`, `delete`), así que los controladores usan los repositorios
  directamente.

Códigos: **200** GET y PUT, **201** POST, **204** DELETE, **404** si no existe el id o alguna referencia, **409** en violación de clave foránea.

### /asociaciones
| Método | URL |
|---|---|
| GET lista | `http://localhost:8082/asociaciones` |
| GET uno | `http://localhost:8082/asociaciones/1` |
| POST | `http://localhost:8082/asociaciones` |
| PUT | `http://localhost:8082/asociaciones/1` |
| DELETE | `http://localhost:8082/asociaciones/1` |

```json
{ "nombre": "FCF", "pais": "Colombia", "presidente": "Ramón Jesurún" }
```

### /entrenadores
| Método | URL |
|---|---|
| GET lista | `http://localhost:8082/entrenadores` |
| GET uno | `http://localhost:8082/entrenadores/1` |
| POST | `http://localhost:8082/entrenadores` |
| PUT | `http://localhost:8082/entrenadores/1` |
| DELETE | `http://localhost:8082/entrenadores/1` |

```json
{ "nombre": "David", "apellido": "González", "edad": 45, "nacionalidad": "Colombiano" }
```

### /competiciones
| Método | URL |
|---|---|
| GET lista | `http://localhost:8082/competiciones` |
| GET uno | `http://localhost:8082/competiciones/1` |
| POST | `http://localhost:8082/competiciones` |
| PUT | `http://localhost:8082/competiciones/1` |
| DELETE | `http://localhost:8082/competiciones/1` |

```json
{ "nombre": "Liga BetPlay", "montoPremio": 15000000, "fechaInicio": "2026-02-01", "fechaFin": "2026-11-15" }
```

### /jugadores
| Método | URL |
|---|---|
| GET lista | `http://localhost:8082/jugadores` |
| GET uno | `http://localhost:8082/jugadores/1` |
| POST | `http://localhost:8082/jugadores` |
| PUT | `http://localhost:8082/jugadores/1` |
| DELETE | `http://localhost:8082/jugadores/1` |

```json
{ "nombre": "Andrés", "apellido": "Mora", "numero": 9, "posicion": "Delantero" }
```

Nota: el jugador se crea **sin club**; para asignarlo: `POST /clubes/{id}/jugadores/{jugadorId}`.
Si el jugador pertenece a un club, `DELETE /jugadores/{id}` responde **409**.

### /clubes
| Método | URL |
|---|---|
| GET lista | `http://localhost:8082/clubes` |
| GET uno | `http://localhost:8082/clubes/1` |
| POST | `http://localhost:8082/clubes` |
| PUT | `http://localhost:8082/clubes/1` |
| DELETE | `http://localhost:8082/clubes/1` |
| POST añade jugador | `http://localhost:8082/clubes/1/jugadores/12` |
| DELETE quita jugador | `http://localhost:8082/clubes/1/jugadores/12` |

Crear o editar (los ids que no existan responden **404**):

```json
{
  "nombre": "Millonarios",
  "idEntrenador": 1,
  "idAsociacion": 1,
  "idsCompeticiones": [1, 2, 3]
}
```

Respuesta de un club:

```json
{
  "id": 1,
  "nombre": "Millonarios",
  "entrenador": { "id": 1, "nombre": "David", "apellido": "González", "edad": 45, "nacionalidad": "Colombiano" },
  "asociacion": { "id": 1, "nombre": "FCF", "pais": "Colombia", "presidente": "Ramón Jesurún" },
  "jugadores": [ { "id": 1, "nombre": "Juan", "apellido": "Pérez", "numero": 1, "posicion": "Portero" } ],
  "competiciones": [ { "id": 1, "nombre": "Liga BetPlay", "montoPremio": 15000000, "fechaInicio": "2026-02-01", "fechaFin": "2026-11-15" } ]
}
```

## Despliegue en la nube (Render)

Deploy en Render (plan gratis) desde GitHub con el `Dockerfile` multi-etapa de la raíz: etapa de build con Maven/JDK 21 (`maven:3-eclipse-temurin-21`, `mvn -q -DskipTests clean package`) e imagen final `eclipse-temurin:21-jre` que copia el jar como `app.jar` y lo arranca con `java -jar app.jar`. `EXPOSE 8118` y la JVM queda limitada con `JAVA_TOOL_OPTIONS=-Xmx300m`.

### Qué hace el perfil cloud

- Se activa con la variable de entorno **`SPRING_PROFILES_ACTIVE=cloud`** (configúrala en el servicio de Render; sin esa variable, en local no cambia nada).
- La base pasa a ser **H2 en memoria** (`application-cloud.properties`): **se reinicia vacía en cada arranque del servicio** y `DatosPruebaRunner` la repuebla automáticamente con los datos de prueba.
- La **consola H2 queda desactivada** en la nube: `https://<servicio>/h2-console` responde 404.
- El puerto lo define Render con la variable `PORT` (`server.port=${PORT:8118}`), así que el local sigue siendo 8118.

### Pasos para desplegar en Render

1. Sube el proyecto a un repositorio de GitHub (sin jar ni BD: el `.gitignore` ya los excluye).
2. En Render: **New → Web Service → "Build and deploy from a Git repository"**, elige el repo.
3. Render detecta el `Dockerfile` automáticamente; deja el Docker Command vacío.
4. Configura la variable de entorno **`SPRING_PROFILES_ACTIVE=cloud`** (Environment → Add).
5. Elige el plan **Free**, crea el servicio y espera el deploy.

URL pública de la aplicación: **[Pendiente: pegar aquí la URL del servicio de Render]**
