# Backend — Ferretería Los Maestros

API REST de la tienda online de Ferretería Los Maestros, proyecto semestral de DSY1104 (EP3). Expone el catálogo, el checkout, la gestión de pedidos y la administración de productos y usuarios, con autenticación JWT y permisos por rol.

El frontend (React + Vite) que consume esta API está en [ferreteria-los-maestros-frontend](https://github.com/andresjp23/ferreteria-los-maestros-frontend).

## Stack

- Java 21 y Spring Boot 4.1 (Spring Web MVC, Spring Data JPA, Bean Validation)
- Spring Security + JWT con [jjwt](https://github.com/jwtk/jjwt) (HS256); contraseñas con BCrypt
- MySQL 8.4 en Docker (`docker-compose.yml`); H2 en memoria solo para los tests
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito y MockMvc

## Estructura

Monolito por capas en el paquete `cl.losmaestros.ferreteria`:

```text
src/main/java/cl/losmaestros/ferreteria/
├── controller/   # Endpoints REST: reciben la petición y devuelven DTOs
├── service/      # Reglas de negocio (stock, total del pedido, estados, validaciones)
├── repository/   # Acceso a la BD con Spring Data JPA
├── model/        # Entidades JPA: Usuario, Producto, Categoria, Pedido, DetallePedido...
├── dto/          # Objetos de entrada y salida de la API (records)
├── validation/   # Anotaciones de validación propias: @ContrasenaSegura y @NombreCompleto
├── security/     # Filtro JWT, generación y validación del token
├── exception/    # Excepciones de negocio y manejador global de errores (400/401/403/404/409)
└── config/       # Seguridad y CORS (SecurityConfig), Swagger y carga de datos iniciales
```

## Requisitos

- **JDK 21.** En macOS, si hay otra versión por defecto: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
- **Docker Desktop** para MySQL (o XAMPP, ver más abajo)
- Internet en la primera ejecución, para descargar Maven y las dependencias

## Puesta en marcha

```bash
cd backend
cp .env.example .env          # luego reemplazar JWT_SECRET por una clave propia
                              # (se puede generar con: openssl rand -base64 48)
docker compose up -d          # MySQL 8.4 con la BD "ferreteria" en el puerto 3306
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

En la primera ejecución el backend crea las tablas y carga los datos de demostración: 9 categorías, 76 productos, 42 usuarios y 15 pedidos. Hay que esperar el mensaje `Datos iniciales: 42 usuarios` en la consola antes de iniciar sesión. En los arranques siguientes el mensaje no aparece, porque los datos ya existen.

| Servicio | URL |
| --- | --- |
| API REST | `http://localhost:8080/api/...` |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |

Después se levanta el frontend (`npm install && npm run dev` en su repositorio) y se abre <http://localhost:5173>.

- **Apagar:** `Ctrl + C` en la terminal del backend y `docker compose stop` para MySQL (los datos se conservan).
- **Volver a los datos iniciales:** `docker compose down -v && docker compose up -d`. Borra todo lo creado.
- **Con XAMPP en vez de Docker:** crear la base `ferreteria` y un usuario, y poner esos datos en `DB_URL`, `DB_USER` y `DB_PASSWORD` dentro de `.env`.

## Variables de entorno

Se leen desde `backend/.env`, que no se sube al repositorio (`.env.example` es la plantilla).

| Variable | Por defecto | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/ferreteria?...` | Conexión JDBC |
| `DB_USER` / `DB_PASSWORD` | `ferreteria` / `ferreteria` | Credenciales de la BD (solo desarrollo) |
| `JWT_SECRET` | — (obligatoria) | Clave para firmar los JWT, de al menos 32 caracteres |
| `JWT_EXPIRATION_MINUTES` | `60` | Duración de la sesión. Con 1 o 2 se puede demostrar la expiración |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes del frontend, separados por coma |
| `SERVER_PORT` | `8080` | Puerto HTTP |

## Cuentas de demostración

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@losmaestros.cl` | `admin123` |
| Vendedor | `vendedor@losmaestros.cl` | `vendedor123` |
| Cliente | `cliente@losmaestros.cl` | `cliente123` |

Las demás cuentas de ejemplo usan la contraseña `Maestros2026`. Son credenciales de demostración para el entorno local, no secretos reales.

## Autenticación y permisos

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

## Tests

```bash
./mvnw test    # usan H2 en memoria (perfil "test"), no necesitan MySQL
```

55 tests: pruebas de la API con MockMvc (login, catálogo, autorización por rol, usuarios y pedidos) y pruebas unitarias de las reglas de negocio (productos, stock y estados del pedido).

## Documentación

- [Documento de apoyo para la defensa técnica](docs/documento-defensa-backend.md): entidades, servicios, Swagger, JWT y permisos, con la ubicación de cada parte en el código.
- [`Tasks.md`](Tasks.md): plan de trabajo y estado de cada tarea del backend.
