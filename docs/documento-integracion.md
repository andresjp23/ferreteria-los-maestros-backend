# Documento de integración frontend–backend

**Ferretería Los Maestros** · DSY1104 Desarrollo Fullstack II · EP3 «Integrando backend y frontend a la tienda online»
Sección 005D · Autor: Andres Jordan · Noviembre 2026

| Repositorio | Enlace |
| --- | --- |
| Frontend (React + Vite) | <https://github.com/andresjp23/ferreteria-los-maestros-frontend> |
| Backend (Spring Boot + MySQL) | <https://github.com/andresjp23/ferreteria-los-maestros-backend> |

---

## Índice

1. [Resumen](#1-resumen)
2. [Arquitectura](#2-arquitectura)
3. [Componentes del backend](#3-componentes-del-backend)
4. [Componentes del frontend](#4-componentes-del-frontend)
5. [Flujo de una petición REST: el checkout](#5-flujo-de-una-petición-rest-el-checkout)
6. [Endpoints y permisos](#6-endpoints-y-permisos)
7. [Autenticación con JWT, roles y sesión](#7-autenticación-con-jwt-roles-y-sesión)
8. [Manejo de errores](#8-manejo-de-errores)
9. [Configuración y pasos para ejecutar](#9-configuración-y-pasos-para-ejecutar)
10. [Pruebas de integración](#10-pruebas-de-integración)
11. [Limitaciones y mejoras posibles](#11-limitaciones-y-mejoras-posibles)

---

## 1. Resumen

La tienda online de la EP2 funcionaba con datos simulados (*mocks*) dentro del navegador. En la EP3 esos datos pasan a un **backend Spring Boot** con una **base de datos MySQL**, y el frontend React los consume mediante una **API REST**:

- El catálogo, los usuarios y los pedidos se guardan en MySQL.
- El inicio de sesión devuelve un **JWT**. Cada petición protegida lo envía y el backend comprueba el **rol** antes de responder.
- La sesión se conserva al recargar la página y se cierra sola cuando el token vence.
- El frontend muestra u oculta pantallas según el rol, pero **quien protege los datos es el backend**: si alguien llama a la API directamente, recibe 401 o 403.

**Tecnologías**

| Capa | Tecnología |
| --- | --- |
| Frontend | React 19, React Router 8, Vite 8, Tailwind CSS 4 |
| Backend | Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Security) |
| Autenticación | JWT con la librería jjwt, firmado con HS256. Contraseñas con BCrypt |
| Base de datos | MySQL 8.4 en Docker (H2 en memoria solo para los tests) |
| Documentación de la API | springdoc-openapi (Swagger UI) |

---

## 2. Arquitectura

El backend es un **monolito por capas**: un solo proyecto Spring Boot en el puerto 8080. El frontend es una SPA que corre en el puerto 5173 y se comunica con el backend solo por HTTP con JSON.

```mermaid
flowchart LR
    subgraph Navegador["Navegador · localhost:5173"]
        UI["Páginas y componentes React"]
        CTX["Providers<br/>(Auth, Catalog, Cart)"]
        SRV["services/*.js"]
        API["services/api.js<br/>(fetch + Bearer token)"]
        LS[("localStorage<br/>auth_token · cart")]
        UI --> CTX --> SRV --> API
        API <--> LS
    end

    subgraph Backend["Spring Boot · localhost:8080"]
        F["JwtAuthFilter +<br/>SecurityFilterChain"]
        C["Controllers<br/>(@RestController)"]
        S["Services<br/>(reglas de negocio)"]
        R["Repositories<br/>(Spring Data JPA)"]
        F --> C --> S --> R
    end

    DB[("MySQL 8.4<br/>BD ferreteria")]

    API -- "HTTP + JSON<br/>Authorization: Bearer &lt;JWT&gt;" --> F
    R -- "JDBC / Hibernate" --> DB
```

**Por qué un monolito:** el alcance de la EP3 (catálogo, usuarios, pedidos y autenticación) cabe en una sola aplicación. Separarla en microservicios agregaría un gateway, varias bases de datos y comunicación entre servicios sin ningún beneficio para el caso. Las capas igual quedan separadas por paquete.

**Modelo de datos**

```mermaid
erDiagram
    CATEGORIAS ||--o{ PRODUCTOS : agrupa
    USUARIOS ||--o{ PEDIDOS : realiza
    PEDIDOS ||--|{ DETALLES_PEDIDO : contiene
    PRODUCTOS ||--o{ DETALLES_PEDIDO : "aparece en"

    CATEGORIAS { string id PK "ej. herramientas" string nombre }
    PRODUCTOS { string codigo PK "ej. MC001" string nombre int precioCompra int precio int stock int stockMinimo string imagen boolean activo }
    USUARIOS { long id PK string nombre string email UK string passwordHash string rol string estado }
    PEDIDOS { long id PK datetime fecha string estado long total }
    DETALLES_PEDIDO { long id PK string nombreProducto int precioUnitario int cantidad long subtotal }
```

- `DETALLES_PEDIDO` guarda el **nombre y el precio al momento de la compra**. Si después cambia el precio del producto, los pedidos antiguos no se alteran.
- Los productos eliminados se **desactivan** (`activo = false`) en vez de borrarse, para no romper los pedidos que los incluyen.

---

## 3. Componentes del backend

Paquete base: `cl.losmaestros.ferreteria`.

| Paquete | Responsabilidad | Clases principales |
| --- | --- | --- |
| `model` | Entidades JPA (tablas) y enums | `Producto`, `Categoria`, `Usuario`, `Pedido`, `DetallePedido`, `Rol`, `EstadoPedido` |
| `repository` | Acceso a la BD con Spring Data JPA | `ProductoRepository` (incluye `bloquearActivos` con `SELECT … FOR UPDATE`), `UsuarioRepository`, `PedidoRepository`, `CategoriaRepository` |
| `service` | Reglas de negocio y transacciones | `ProductoService`, `UsuarioService`, `PedidoService`, `AuthService`, `ResumenService` |
| `controller` | Endpoints REST: reciben DTOs y devuelven DTOs | `ProductoController`, `AdminProductoController`, `UsuarioController`, `PedidoController`, `AuthController`, `AdminController`, `CatalogoController` |
| `dto` | Objetos de entrada (`*Request`, con Bean Validation) y de salida (`*Response`) | `ProductoRequest`, `UsuarioRequest`, `CrearPedidoRequest`, `LoginResponse`… |
| `security` | Generación y validación del JWT y respuestas 401/403 en JSON | `JwtService`, `JwtAuthFilter`, `RestAuthHandlers` |
| `config` | Seguridad, CORS, Swagger y carga de datos iniciales | `SecurityConfig`, `OpenApiConfig`, `DatosIniciales` |
| `exception` | Excepciones de negocio y su traducción a JSON | `GlobalExceptionHandler`, `ConflictoException`, `ReglaNegocioException`… |
| `validation` | Validaciones propias | `@NombreCompleto`, `@ContrasenaSegura` |

**Reglas de negocio que aplica el backend**

| Regla | Dónde | Respuesta si no se cumple |
| --- | --- | --- |
| El precio de venta no puede ser menor que el de compra | `ProductoService` | 400 en `precioVenta` |
| El código de producto y el correo de usuario son únicos | `ProductoService`, `UsuarioService` | 409 |
| El checkout valida y descuenta el stock en **una sola transacción**, con los productos bloqueados | `PedidoService.crear` | 409 «Stock insuficiente…» |
| El precio y el total del pedido los calcula el servidor (el navegador solo envía `id` y `cantidad`) | `PedidoService.crear` | — |
| El estado de un pedido solo avanza un paso: En preparación → Enviado → Entregado | `PedidoService.cambiarEstado` | 400 |
| El administrador no puede eliminar ni desactivar su propia cuenta, ni quitarse el rol de admin | `UsuarioService` | 400 |
| Un usuario con pedidos no se puede eliminar (se desactiva) | `UsuarioService.eliminar` | 409 |
| Las contraseñas se guardan con BCrypt y nunca se devuelven en las respuestas | `UsuarioService`, DTOs | — |

**Datos iniciales:** `DatosIniciales` carga los archivos `src/main/resources/datos/*.json` (9 categorías, 76 productos, 42 usuarios y 15 pedidos) **solo si las tablas están vacías**. Por eso reiniciar el backend no duplica los datos.

---

## 4. Componentes del frontend

| Carpeta / archivo | Responsabilidad |
| --- | --- |
| `src/services/api.js` | **Cliente HTTP único.** Arma la URL (`VITE_API_URL`), envía y recibe JSON, agrega `Authorization: Bearer <token>` y convierte los errores del backend en `ApiError { status, mensaje, campos }`. Ante un 401 con sesión, avisa al `AuthProvider` para cerrarla |
| `src/services/*Service.js` | Una función por endpoint: `authService` (login y `/me`), `catalogService`, `orderService`, `userService`, `adminService` |
| `src/context/AuthProvider.jsx` | Sesión: guarda el token, recupera el usuario con `/auth/me` al recargar y programa el cierre automático cuando el token vence |
| `src/context/CatalogProvider.jsx` | Productos y categorías traídos del backend, compartidos por todas las vistas. Se recargan después de una compra |
| `src/context/CartProvider.jsx` | Carrito en `localStorage`, guardado solo como `{ id, cantidad }`. El precio y el stock se leen del catálogo actual |
| `src/components/auth/ProtectedRoute.jsx` | Protege las rutas `/admin/*` y `/vendedor/*` según el rol |
| `src/pages/admin/*` | Dashboard (`/admin/resumen`), CRUD de productos y CRUD de usuarios |
| `src/pages/seller/Orders.jsx` | Panel del vendedor: lista de pedidos y cambio de estado |
| `src/components/cart/CartSummary.jsx` | Checkout: `POST /pedidos` |

En todos los formularios, los errores por campo que devuelve el backend (`campos.precioVenta`, `campos.email`…) se muestran **debajo del input correspondiente**, porque los nombres coinciden con los del formulario.

---

## 5. Flujo de una petición REST: el checkout

Ejemplo real: el cliente compra 2 sacos de cemento (prueba [P17](#p17--checkout-del-cliente--201)).

```mermaid
sequenceDiagram
    autonumber
    actor U as Cliente
    participant CS as CartSummary.jsx
    participant API as api.js
    participant F as JwtAuthFilter
    participant SC as SecurityConfig
    participant PC as PedidoController
    participant PS as PedidoService
    participant PR as ProductoRepository
    participant DB as MySQL

    U->>CS: Clic en "Continuar con la compra"
    CS->>API: createOrder([{ id: "MC001", cantidad: 2 }])
    API->>F: POST /api/pedidos + Authorization: Bearer <JWT>
    F->>F: Verifica firma y vencimiento.<br/>Carga el usuario de la BD (activo)
    F->>SC: Usuario autenticado con ROLE_CLIENTE
    SC->>PC: POST /api/pedidos exige rol CLIENTE ✔
    PC->>PC: @Valid CrearPedidoRequest
    PC->>PS: crear(clienteId, request)
    PS->>PR: bloquearActivos(["MC001"])
    PR->>DB: SELECT … FOR UPDATE
    DB-->>PR: Producto (stock 79)
    PS->>PS: Valida stock, descuenta 2,<br/>calcula total con el precio del servidor
    PS->>DB: INSERT pedido y detalle, UPDATE stock (commit)
    PS-->>PC: PedidoResponse
    PC-->>API: 201 Created { id: 20, estado: "En preparación", total: 11980 }
    API-->>CS: Pedido creado
    CS->>CS: clearCart() y reloadCatalog()
    CS-->>U: "¡Pedido #20 creado!"
```

**Dónde se actualiza el estado en React:** `CartSummary.jsx` guarda el pedido creado en su estado local (`setOrder`), vacía el carrito (`clearCart`, que también limpia `localStorage`) y pide de nuevo el catálogo (`reloadCatalog`) para que el stock nuevo se vea en todas las páginas.

**Si falla:** con stock insuficiente, el service lanza `ConflictoException` y la transacción se revierte, así que no se crea el pedido ni se descuenta nada. El backend responde 409 con el mensaje, `CartSummary` lo muestra y recarga el catálogo.

---

## 6. Endpoints y permisos

Base: `http://localhost:8080/api`. Todos están documentados en Swagger: <http://localhost:8080/swagger-ui.html>.

| Método | Ruta | Acceso | Uso en el frontend | Respuestas |
| --- | --- | --- | --- | --- |
| POST | `/auth/login` | Público | Formulario de login | 200, 400, 401 |
| GET | `/auth/me` | Con sesión | Restaurar la sesión al recargar | 200, 401 |
| GET | `/productos` · `/productos/{codigo}` | Público | Catálogo y detalle | 200, 404 |
| GET | `/categorias` · `/marcas` · `/unidades` | Público | Filtros del catálogo y formularios | 200 |
| GET | `/admin/productos` · `/admin/productos/{codigo}` | Admin | Tabla y edición (incluye `precioCompra`) | 200, 401, 403, 404 |
| POST | `/admin/productos` | Admin | Agregar producto | 201, 400, 409 |
| PUT | `/admin/productos/{codigo}` | Admin | Editar producto | 200, 400, 404 |
| DELETE | `/admin/productos/{codigo}` | Admin | Eliminar (desactivar) producto | 204, 404 |
| GET | `/admin/resumen` | Admin | Indicadores del dashboard | 200 |
| GET | `/usuarios` · `/usuarios/{id}` | Admin | Tabla y edición de usuarios | 200, 404 |
| POST | `/usuarios` | Admin | Agregar usuario | 201, 400, 409 |
| PUT | `/usuarios/{id}` | Admin | Editar usuario (contraseña opcional) | 200, 400, 409 |
| DELETE | `/usuarios/{id}` | Admin | Eliminar usuario | 204, 400, 409 |
| POST | `/pedidos` | Cliente | Checkout del carrito | 201, 400, 409 |
| GET | `/pedidos/mios` | Cliente | Pedidos propios | 200 |
| GET | `/pedidos` (`?limit=`) · `/pedidos/{id}` | Vendedor, Admin | Panel del vendedor y pedidos recientes del dashboard | 200, 404 |
| PATCH | `/pedidos/{id}/estado` | Vendedor | Cambiar estado / Enviar | 200, 400 |

Sin token, todas las rutas que no son públicas responden **401**. Con un rol que no corresponde responden **403**.

Las reglas están en `SecurityConfig.java`, por ejemplo:

```java
.requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CLIENTE")
.requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado").hasRole("VENDEDOR")
.requestMatchers("/api/admin/**", "/api/usuarios/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

---

## 7. Autenticación con JWT, roles y sesión

### 7.1 Dónde se crea el token

1. `POST /api/auth/login` → `AuthService` busca el usuario por correo y compara la contraseña con `BCryptPasswordEncoder`.
2. Si es correcta, `JwtService.generar()` crea un JWT **firmado con HS256** usando la clave `JWT_SECRET`. Contiene:
   - `sub`: id del usuario
   - `email` y `rol`
   - `iat` (emitido) y `exp` (vence a los `JWT_EXPIRATION_MINUTES`, 60 por defecto)
3. Responde `{ token, expiraEn, usuario }`. El usuario no incluye la contraseña.

### 7.2 Dónde se valida y cómo se verifica el rol

En **cada petición**, `JwtAuthFilter` (un `OncePerRequestFilter`):

1. Lee el header `Authorization: Bearer <token>`.
2. `JwtService.validar()` comprueba la **firma** y el **vencimiento**.
3. Busca al usuario en la BD y verifica que siga **activo**. El rol se toma **de la BD**, no del token: si el administrador cambia el rol de alguien o lo desactiva, el cambio aplica de inmediato.
4. Registra el usuario en el `SecurityContext` con su autoridad (`ROLE_ADMIN`, `ROLE_VENDEDOR` o `ROLE_CLIENTE`).

Después, `SecurityConfig` decide con `hasRole(...)` si deja pasar la petición. Si no hay usuario autenticado, `RestAuthHandlers` responde **401** con el motivo («Tu sesión expiró…», «Sesión inválida…»). Si el rol no alcanza, responde **403**.

La política de sesión es `STATELESS`: el servidor no guarda sesiones, y toda la información viaja en el token.

### 7.3 Gestión de la sesión en el frontend

```mermaid
flowchart TD
    A["Abrir o recargar la app"] --> B{"¿Hay auth_token<br/>en localStorage?"}
    B -- No --> Z["Visitante sin sesión"]
    B -- Sí --> C{"¿El token ya venció?<br/>(se lee exp del JWT)"}
    C -- Sí --> D["Borra el token<br/>→ login con 'Tu sesión expiró'"]
    C -- No --> E["GET /api/auth/me<br/>(ProtectedRoute muestra un loader)"]
    E -- 200 --> F["setUser(usuario)<br/>Sesión restaurada"]
    E -- 401 --> D
    F --> G["Programa el cierre automático<br/>para el momento en que vence el token"]
    F --> H["Cualquier petición con 401<br/>→ logout + mensaje"]
```

- **Login:** `AuthProvider.login()` guarda el token en `localStorage` (`auth_token`) y el usuario en el estado de React.
- **Recarga:** el token sigue en `localStorage` y el usuario se pide de nuevo a `/auth/me`, de modo que **el rol siempre lo dice el servidor**. Mientras llega la respuesta, `ProtectedRoute` muestra un loader para no mandar al login antes de tiempo (prueba [P23](#p23--la-sesión-se-conserva-al-recargar)).
- **Expiración:** se controla por tres vías: (1) al abrir la app, si el token guardado ya venció se descarta; (2) un `setTimeout` cierra la sesión justo cuando vence; (3) si el backend responde 401 a una petición con token, `api.js` llama a `logout`. En los tres casos el login muestra «Tu sesión expiró. Inicia sesión nuevamente».
- **Logout:** borra el token y el usuario.

### 7.4 Control de acceso a interfaces y acciones

En el frontend:
- `ProtectedRoute allowedRoles={["admin"]}` envuelve `/admin/*` y `allowedRoles={["vendedor"]}` envuelve `/vendedor/*`. Sin sesión, redirige a `/login` y recuerda la página que se quería abrir. Con otro rol, lo manda a la página de inicio de su propio rol.
- La navbar y los botones cambian según el rol: el checkout pide iniciar sesión como cliente (a admin y vendedor les muestra el botón deshabilitado con «Las compras se hacen con una cuenta de cliente»), y los botones de editar o eliminar solo existen en los paneles.

**Por qué el backend también valida:** el frontend corre en el navegador del usuario, así que cualquiera puede saltarse una pantalla escribiendo la URL o llamar a la API directamente con curl, Postman o Swagger. Ocultar un botón no protege nada. Por eso cada endpoint revisa el token y el rol en el servidor. Las pruebas P09, P15, P19 y P22 lo demuestran: con un token válido pero del rol equivocado, la API responde 403.

---

## 8. Manejo de errores

Todas las respuestas de error tienen el mismo formato JSON, generado por `GlobalExceptionHandler` y `RestAuthHandlers`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "mensaje": "No puede ser menor al precio de compra",
  "campos": { "precioVenta": "No puede ser menor al precio de compra" }
}
```

| Código | Cuándo | Origen |
| --- | --- | --- |
| 400 | Falla Bean Validation (`@NotBlank`, `@Pattern`…) o una regla de negocio | `MethodArgumentNotValidException`, `ReglaNegocioException` |
| 401 | Sin token, token alterado o vencido, o credenciales incorrectas | `RestAuthHandlers`, `NoAutorizadoException` |
| 403 | Token válido, pero el rol no tiene permiso | `RestAuthHandlers` |
| 404 | El recurso no existe | `RecursoNoEncontradoException` |
| 409 | Código o correo duplicado, stock insuficiente o usuario con pedidos | `ConflictoException` |

En el frontend, `api.js` convierte esta respuesta en un `ApiError`: `mensaje` se muestra como aviso general y `campos` se reparte entre los inputs del formulario. Si el backend está apagado, `fetch` falla y se muestra «No se pudo conectar con el servidor».

---

## 9. Configuración y pasos para ejecutar

### 9.1 Requisitos

- **JDK 21.** En macOS, si hay otra versión por defecto: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
- **Node.js** 20.19 o superior, o 22.12 o superior
- **Docker Desktop** para MySQL (o XAMPP con una BD `ferreteria` creada a mano)
- Internet en la primera ejecución, para descargar las dependencias de Maven y npm

### 9.2 Variables de entorno

**Backend** (`backend/.env`, copiado desde `.env.example`; no se sube al repositorio):

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/ferreteria?...` | Conexión a MySQL |
| `DB_USER` / `DB_PASSWORD` | `ferreteria` / `ferreteria` | Credenciales de la BD (solo desarrollo) |
| `JWT_SECRET` | — (obligatoria) | Clave para firmar los JWT, de al menos 32 caracteres. Se genera con `openssl rand -base64 48` |
| `JWT_EXPIRATION_MINUTES` | `60` | Duración de la sesión. Con 1 o 2 se puede demostrar la expiración |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes que pueden llamar a la API desde el navegador |

**Frontend** (`frontend/.env.development`, ya incluido): `VITE_API_URL=http://localhost:8080/api`.

### 9.3 Orden de arranque

```bash
# 1. Base de datos
cd backend
cp .env.example .env            # y reemplazar JWT_SECRET por una clave propia
docker compose up -d            # MySQL 8.4 en el puerto 3306

# 2. Backend (puerto 8080)
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
# Esperar en la consola el mensaje "Datos iniciales: 42 usuarios"

# 3. Frontend (puerto 5173), en otra terminal
cd frontend
npm install
npm run dev
```

| Servicio | URL |
| --- | --- |
| Tienda (frontend) | <http://localhost:5173> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| API REST | `http://localhost:8080/api/...` |

En el primer arranque, Hibernate crea las tablas (`ddl-auto=update`) y `DatosIniciales` carga los datos de ejemplo. Para volver a los datos iniciales: `docker compose down -v && docker compose up -d`.

### 9.4 Cuentas de demostración

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@losmaestros.cl` | `admin123` |
| Vendedor | `vendedor@losmaestros.cl` | `vendedor123` |
| Cliente | `cliente@losmaestros.cl` | `cliente123` |

Son credenciales de demostración solo para el entorno local, no secretos reales.

### 9.5 Tests automatizados

Además de las pruebas manuales de la sección 10, ambos proyectos tienen tests automatizados:
- Backend: `./mvnw test` (JUnit 5, Mockito y MockMvc; perfil `test` con H2 en memoria).
- Frontend: `npm test` (Vitest y Testing Library, con un `fetch` simulado).

---

## 10. Pruebas de integración

Se hicieron el **11 de noviembre de 2026** contra el backend local conectado a MySQL, usando **Swagger UI** (P01–P22) y el **frontend** (P23). Cada prueba tiene dos capturas: la petición enviada (*body*) y la respuesta del servidor (*response*).

> Los tokens que aparecen en algunas capturas fueron emitidos por el entorno local de desarrollo, con una clave que no está publicada, y ya expiraron.

**Cómo se autentica en Swagger:** se hace login con `POST /api/auth/login`, se copia el `token` y se pega en el botón **Authorize**. Para probar sin sesión, se usa **Authorize → Logout**.

### 10.1 Resumen

| # | Caso | Usuario | Petición | Esperado | Obtenido |
| --- | --- | --- | --- | --- | --- |
| P01 | Login correcto | admin | `POST /auth/login` | 200 + token | ✅ 200 |
| P02 | Contraseña incorrecta | — | `POST /auth/login` | 401 | ✅ 401 |
| P03 | Campos vacíos | — | `POST /auth/login` | 400 con error por campo | ✅ 400 |
| P04 | Catálogo sin sesión | — | `GET /productos` | 200 | ✅ 200 |
| P05 | Producto inexistente | — | `GET /productos/ZZ999` | 404 | ✅ 404 |
| P06 | Restaurar sesión | cliente | `GET /auth/me` | 200 con el usuario | ✅ 200 |
| P07 | Token inválido | — | `GET /auth/me` | 401 | ✅ 401 |
| P08 | Usuarios sin token | — | `GET /usuarios` | 401 | ✅ 401 |
| P09 | Usuarios como cliente | cliente | `GET /usuarios` | 403 | ✅ 403 |
| P10 | Usuarios como admin | admin | `GET /usuarios` | 200 sin contraseñas | ✅ 200 |
| P11 | Crear producto | admin | `POST /admin/productos` | 201 | ✅ 201 |
| P12 | Código repetido | admin | `POST /admin/productos` | 409 | ✅ 409 |
| P13 | Venta menor que compra | admin | `POST /admin/productos` | 400 en `precioVenta` | ✅ 400 |
| P14 | Editar producto | admin | `PUT /admin/productos/HE901` | 200 | ✅ 200 |
| P15 | Crear producto como vendedor | vendedor | `POST /admin/productos` | 403 | ✅ 403 |
| P16 | Eliminar producto | admin | `DELETE /admin/productos/HE901` | 204 | ✅ 204 |
| P17 | Checkout | cliente | `POST /pedidos` | 201 | ✅ 201 |
| P18 | Stock insuficiente | cliente | `POST /pedidos` | 409 | ✅ 409 |
| P19 | Checkout como vendedor | vendedor | `POST /pedidos` | 403 | ✅ 403 |
| P20 | Avanzar estado | vendedor | `PATCH /pedidos/20/estado` | 200 | ✅ 200 |
| P21 | Retroceder estado | vendedor | `PATCH /pedidos/20/estado` | 400 | ✅ 400 |
| P22 | Cambiar estado como admin | admin | `PATCH /pedidos/20/estado` | 403 | ✅ 403 |
| P23 | Sesión conservada al recargar | admin | F5 en `/admin` | Sigue en el panel | ✅ |

**Resultado: 23 de 23 pruebas con el resultado esperado.** Cubren el CRUD completo de productos, tres roles con permisos distintos, respuestas 401 y 403, validaciones, reglas de negocio y persistencia de la sesión.

### 10.2 Detalle de cada prueba

#### P01 · Login correcto → 200

- **Pasos:** `Autenticación → POST /api/auth/login` con `{ "email": "admin@losmaestros.cl", "password": "admin123" }`.
- **Esperado:** 200 con `token`, `expiraEn` y `usuario` con rol `admin`, sin contraseña.
- **Obtenido:** 200. El usuario viene sin contraseña.

![P01 body](capturas/P01_body.png)
![P01 response](capturas/P01_response.png)

#### P02 · Contraseña incorrecta → 401

- **Pasos:** mismo endpoint, con `"password": "mala"`.
- **Esperado:** 401 «Correo o contraseña incorrectos».
- **Obtenido:** 401 con ese mensaje. No indica si lo incorrecto es el correo o la contraseña.

![P02 body](capturas/P02_body.png)
![P02 response](capturas/P02_response.png)

#### P03 · Campos vacíos → 400

- **Pasos:** mismo endpoint, con `{ "email": "", "password": "" }`.
- **Esperado:** 400 con un error en `campos.email` y otro en `campos.password`.
- **Obtenido:** 400 con «El correo es obligatorio» y «La contraseña es obligatoria».

![P03 body](capturas/P03_body.png)
![P03 response](capturas/P03_response.png)

#### P04 · Catálogo sin sesión → 200

- **Pasos:** sin autorizar, `Catálogo → GET /api/productos`.
- **Esperado:** 200 con la lista de productos activos. El catálogo es público.
- **Obtenido:** 200 con los productos. La respuesta no incluye el precio de compra.

![P04 body](capturas/P04_body.png)
![P04 response](capturas/P04_response.png)

#### P05 · Producto inexistente → 404

- **Pasos:** `GET /api/productos/{codigo}` con `ZZ999`.
- **Esperado:** 404 «Producto no encontrado».
- **Obtenido:** 404.

![P05 body](capturas/P05_body.png)
![P05 response](capturas/P05_response.png)

#### P06 · Restaurar sesión con `/me` → 200

- **Pasos:** login como cliente, autorizar con su token y ejecutar `GET /api/auth/me`.
- **Esperado:** 200 con el usuario dueño del token. El frontend llama a este endpoint al recargar la página.
- **Obtenido:** 200 con «Camila Rojas», rol `cliente`.

![P06 body](capturas/P06_body.png)
![P06 response](capturas/P06_response.png)

#### P07 · Token inválido → 401

- **Pasos:** autorizar con el token `abc.def.ghi` y ejecutar `GET /api/auth/me`.
- **Esperado:** 401 «Sesión inválida. Inicia sesión nuevamente».
- **Obtenido:** 401. `JwtAuthFilter` rechazó la firma.

![P07 body](capturas/P07_body.png)
![P07 response](capturas/P07_response.png)

#### P08 · Usuarios sin token → 401

- **Pasos:** Authorize → Logout y ejecutar `Admin · Usuarios → GET /api/usuarios`.
- **Esperado:** 401 «Debes iniciar sesión para continuar».
- **Obtenido:** 401.

![P08 body](capturas/P08_body.png)
![P08 response](capturas/P08_response.png)

#### P09 · Usuarios como cliente → 403

- **Pasos:** autorizar con el token del cliente y ejecutar `GET /api/usuarios`.
- **Esperado:** 403 «No tienes permisos para realizar esta acción». El cliente está identificado, pero no es admin.
- **Obtenido:** 403.

![P09 body](capturas/P09_body.png)
![P09 response](capturas/P09_response.png)

#### P10 · Usuarios como admin → 200

- **Pasos:** autorizar con el token del admin y ejecutar `GET /api/usuarios`.
- **Esperado:** 200 con la lista de usuarios, sin contraseñas ni hashes.
- **Obtenido:** 200 con los 42 usuarios. Ninguno trae contraseña.

![P10 body](capturas/P10_body.png)
![P10 response](capturas/P10_response.png)

#### P11 · Crear producto → 201

- **Pasos:** como admin, `Admin · Productos → POST /api/admin/productos` con el producto `HE901` («Martillo de prueba», compra 1000, venta 1500, stock 10).
- **Esperado:** 201 con el producto creado.
- **Obtenido:** 201. El producto quedó guardado en MySQL.

![P11 body](capturas/P11_body.png)
![P11 response](capturas/P11_response.png)

#### P12 · Código repetido → 409

- **Pasos:** se envía otra vez el mismo cuerpo de P11.
- **Esperado:** 409 «Ya existe un producto con este código» en `campos.codigo`.
- **Obtenido:** 409.

![P12 body](capturas/P12_body.png)
![P12 response](capturas/P12_response.png)

#### P13 · Precio de venta menor que el de compra → 400

- **Pasos:** cuerpo de P11 con `"codigo": "HE902"` y `"precioVenta": 500`.
- **Esperado:** 400 con `campos.precioVenta = "No puede ser menor al precio de compra"`.
- **Obtenido:** 400. Es la misma regla que valida el formulario del frontend, aplicada también en el servidor.

![P13 body](capturas/P13_body.png)
![P13 response](capturas/P13_response.png)

#### P14 · Editar producto → 200

- **Pasos:** `PUT /api/admin/productos/HE901` con el cuerpo de P11 y `"stock": 25`.
- **Esperado:** 200 con `stock: 25`.
- **Obtenido:** 200 con el stock actualizado.

![P14 body](capturas/P14_body.png)
![P14 response](capturas/P14_response.png)

#### P15 · Crear producto como vendedor → 403

- **Pasos:** autorizar con el token del vendedor y ejecutar `POST /api/admin/productos` (código `HE903`).
- **Esperado:** 403. Solo el admin gestiona el catálogo.
- **Obtenido:** 403. El producto no se creó.

![P15 body](capturas/P15_body.png)
![P15 response](capturas/P15_response.png)

#### P16 · Eliminar producto → 204

- **Pasos:** como admin, `DELETE /api/admin/productos/HE901`.
- **Esperado:** 204 sin cuerpo. El producto deja de aparecer en el catálogo.
- **Obtenido:** 204.

![P16 body](capturas/P16_body.png)
![P16 response](capturas/P16_response.png)

#### P17 · Checkout del cliente → 201

- **Pasos:** como cliente, `Pedidos → POST /api/pedidos` con `{ "items": [ { "id": "MC001", "cantidad": 2 } ] }`.
- **Esperado:** 201 con el pedido en estado «En preparación» y el total calculado por el servidor.
- **Obtenido:** 201, pedido **#20**, total $11.980 (2 × $5.990). El navegador solo envió el código y la cantidad: el precio lo puso el backend.

![P17 body](capturas/P17_body.png)
![P17 response](capturas/P17_response.png)

#### P18 · Stock insuficiente → 409

- **Pasos:** mismo endpoint, con `"cantidad": 1000`.
- **Esperado:** 409 «Stock insuficiente para … quedan N unidades».
- **Obtenido:** 409. No se creó el pedido ni se descontó stock, porque la transacción se revirtió.

![P18 body](capturas/P18_body.png)
![P18 response](capturas/P18_response.png)

#### P19 · Checkout como vendedor → 403

- **Pasos:** autorizar con el token del vendedor y enviar el cuerpo de P17.
- **Esperado:** 403. Solo el rol cliente puede comprar (`hasRole("CLIENTE")`).
- **Obtenido:** 403. Aunque el frontend no muestra esta opción al vendedor, el backend la bloquea igual cuando se llama a la API directamente.

![P19 body](capturas/P19_body.png)
![P19 response](capturas/P19_response.png)

#### P20 · Vendedor avanza el estado → 200

- **Pasos:** como vendedor, `PATCH /api/pedidos/20/estado` con `{ "estado": "Enviado" }`.
- **Esperado:** 200 con `estado: "Enviado"`.
- **Obtenido:** 200.

![P20 body](capturas/P20_body.png)
![P20 response](capturas/P20_response.png)

#### P21 · Retroceder el estado → 400

- **Pasos:** mismo pedido, con `{ "estado": "En preparación" }`.
- **Esperado:** 400 «Un pedido "Enviado" solo puede pasar a "Entregado"».
- **Obtenido:** 400. La regla de negocio está en `PedidoService.cambiarEstado`.

![P21 body](capturas/P21_body.png)
![P21 response](capturas/P21_response.png)

#### P22 · Admin intenta cambiar el estado → 403

- **Pasos:** autorizar con el token del admin y ejecutar `PATCH /api/pedidos/20/estado` con `{ "estado": "Entregado" }`.
- **Esperado:** 403. Cambiar estados es tarea exclusiva del vendedor.
- **Obtenido:** 403. Tener el rol con más permisos no da acceso a todo: cada acción tiene su rol.

![P22 body](capturas/P22_body.png)
![P22 response](capturas/P22_response.png)

#### P23 · La sesión se conserva al recargar

- **Pasos:** en el frontend, iniciar sesión como admin, entrar a `/admin` y recargar con F5, con DevTools → Network abierto.
- **Esperado:** el panel sigue visible con el nombre del admin, sin volver al login. En Network aparece la llamada a `me` (`GET /api/auth/me`) seguida de las de `resumen` y `pedidos?limit=5`.
- **Obtenido:** el panel se recargó con «Administrador General» y los datos del dashboard traídos del backend.

![P23 sesión conservada](capturas/P23.png)

---

## 11. Limitaciones y mejoras posibles

| Limitación | Por qué ocurre | Mejora posible |
| --- | --- | --- |
| Un código de producto eliminado no se puede reutilizar: crear otro con ese código da 409, pero consultarlo da 404 | La eliminación desactiva el producto (`activo = false`) para conservar los pedidos, pero el código sigue ocupado | Reactivar el producto al crear uno con el mismo código, o avisar «existe un producto eliminado con este código» |
| El JWT se guarda en `localStorage` | Es simple y permite conservar la sesión al recargar, pero un script inyectado (XSS) podría leerlo | Guardarlo en una cookie `HttpOnly` + `SameSite`, o usar un token de acceso corto con *refresh token* |
| No hay *refresh token*: al vencer el token hay que volver a iniciar sesión | Se priorizó la simplicidad | Agregar un endpoint `/auth/refresh` |
| No se puede revocar un token antes de que venza | Los JWT son *stateless*. Igual, si se desactiva el usuario, su token deja de servir, porque el filtro lo busca en la BD en cada petición | Lista de tokens revocados o vencimientos más cortos |
| La imagen del producto es una ruta o URL, no un archivo subido | La subida de archivos quedó fuera del alcance | `POST /api/productos/{id}/imagen` con *multipart*, guardando el archivo en disco o en un almacenamiento de objetos |
| Funciona solo en local, por HTTP | La EP3 no exige despliegue | Desplegar con HTTPS y definir `CORS_ORIGINS` y `JWT_SECRET` por entorno |
| El formulario de contacto no envía mensajes | No forma parte de la EP3 | Endpoint `POST /api/contacto` y su listado en el panel de admin |
