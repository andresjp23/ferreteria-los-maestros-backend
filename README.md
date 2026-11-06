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

En la primera ejecución el backend crea las tablas y carga los datos de demostración (9 categorías, 76 productos, 42 usuarios y 15 pedidos). Espera el mensaje `Datos iniciales: 42 usuarios` en la consola antes de iniciar sesión. Para partir de cero: `docker compose down -v && docker compose up -d`.

## Cuentas de demostración

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@losmaestros.cl` | `admin123` |
| Vendedor | `vendedor@losmaestros.cl` | `vendedor123` |
| Cliente | `cliente@losmaestros.cl` | `cliente123` |

Las demás cuentas de ejemplo usan la contraseña `Maestros2026`.

## Autenticación

1. `POST /api/auth/login` con `{ "email", "password" }` devuelve `{ token, expiraEn, usuario }`.
2. Las rutas protegidas se llaman con el header `Authorization: Bearer <token>` (en Swagger: botón **Authorize**).
3. Sin token, o con un token vencido o alterado → **401**. Con un rol que no corresponde → **403**.

| Acceso | Rutas |
| --- | --- |
| Público | `POST /api/auth/login`, `GET /api/productos/**`, `/api/categorias`, `/api/marcas`, `/api/unidades`, Swagger |
| Con sesión | `GET /api/auth/me` |
| Cliente | `POST /api/pedidos` (checkout), `GET /api/pedidos/mios` |
| Vendedor | `GET /api/pedidos`, `GET /api/pedidos/{id}`, `PATCH /api/pedidos/{id}/estado` |
| Administrador | `/api/admin/**` (productos y resumen), `/api/usuarios/**`, `GET /api/pedidos` |


Con XAMPP: crear la base `ferreteria` y un usuario, y poner esos datos en `DB_URL`, `DB_USER` y `DB_PASSWORD` dentro de `.env`.

## Variables de entorno

| Variable | Por defecto | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/ferreteria?...` | Conexión JDBC |
| `DB_USER` / `DB_PASSWORD` | `ferreteria` / `ferreteria` | Credenciales de la BD (solo desarrollo) |
| `JWT_SECRET` | — (obligatoria) | Clave para firmar los JWT, de al menos 32 caracteres |
| `JWT_EXPIRATION_MINUTES` | `60` | Duración de la sesión |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes del frontend, separados por coma |
| `SERVER_PORT` | `8080` | Puerto HTTP |

## Tests

```bash
./mvnw test    # usan H2 en memoria (perfil "test"), no necesitan MySQL
```

El plan de trabajo y el estado de las tareas están en [`Tasks.md`](Tasks.md).
