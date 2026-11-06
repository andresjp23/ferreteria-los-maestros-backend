# Backend — Ferretería Los Maestros

API REST en Spring Boot 4.1 (Java 21) con MySQL 8. Proyecto único por capas (`controller`, `service`, `repository`, `model`, `dto`, `exception`, `config`, `security`) en el paquete `cl.losmaestros.ferreteria`.

El frontend (React + Vite) está en [ferreteria-los-maestros-frontend](https://github.com/andresjp23/ferreteria-los-maestros-frontend).

## Requisitos

- JDK 21 (en macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`)
- MySQL 8: Docker (recomendado) o XAMPP
- La primera ejecución necesita internet para descargar Maven y las dependencias

## Puesta en marcha

```bash
cd backend
cp .env.example .env          # ajustar si se usa otro MySQL
docker compose up -d          # levanta MySQL con la BD "ferreteria"
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

- Swagger: <http://localhost:8080/swagger-ui.html>
- API: `http://localhost:8080/api/...` (endpoints en Swagger y en `Tasks.md`)

Con XAMPP: crear la base `ferreteria` y un usuario, y poner esos datos en `DB_URL`, `DB_USER` y `DB_PASSWORD` dentro de `.env`.

## Variables de entorno

| Variable | Por defecto | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/ferreteria?...` | Conexión JDBC |
| `DB_USER` / `DB_PASSWORD` | `ferreteria` / `ferreteria` | Credenciales de la BD (solo desarrollo) |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes del frontend, separados por coma |
| `SERVER_PORT` | `8080` | Puerto HTTP |

## Tests

```bash
./mvnw test    # usan H2 en memoria (perfil "test"), no necesitan MySQL
```

El plan de trabajo y el estado de las tareas están en [`Tasks.md`](Tasks.md).
