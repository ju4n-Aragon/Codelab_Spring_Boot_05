# cliente-service — Codelab 05 (suite de pruebas)

Spring Boot 3.4 · Java 17 · Maven · JPA · H2 embebido (modo PostgreSQL) · JaCoCo.

## Ejecutar

```bash
mvn spring-boot:run   # http://localhost:8080
mvn test              # corre toda la suite y genera la cobertura
```

Reporte JaCoCo: `target/site/jacoco/index.html`.

## Pirámide de pruebas

| Clase                       | Nivel                    | Herramienta                 |
|-----------------------------|--------------------------|-----------------------------|
| `ClienteServiceImplTest`    | Unit test                | Mockito                     |
| `ClienteRestControllerTest` | Integration (web slice)  | `@WebMvcTest` + MockMvc + `@MockitoBean` |
| `IClienteDaoTest`           | Integration (data slice) | `@DataJpaTest` + H2         |
| `ClienteIntegrationTest`    | E2E                      | `@SpringBootTest` (puerto aleatorio) + `TestRestTemplate` + H2 |

> El E2E usa H2 y **no** Testcontainers: el entorno de calificación no tiene Docker.

## Endpoints (`/api/v1/cliente-service`, versión en `api.version`)

| Método | Ruta                    | Éxito         | Error                         |
|--------|-------------------------|---------------|-------------------------------|
| GET    | `/clientes`             | 200 + array   | —                             |
| GET    | `/clientes/page/{page}` | 200 + Page    | —                             |
| GET    | `/clientes/{id}`        | 200 + Cliente | 404                           |
| POST   | `/clientes`             | 201 + Cliente | 400 (validación)              |
| PUT    | `/clientes/{id}`        | 201 + Cliente | 404 / 400                     |
| DELETE | `/clientes/{id}`        | 204           | 404                           |

Regiones iniciales en `src/main/resources/import.sql`; el `import.sql` de `src/test/resources` está vacío.
