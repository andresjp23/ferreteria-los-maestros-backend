# Backend EP3 — Lista de tareas

Backend de **Ferretería Los Maestros**: un solo proyecto Spring Boot (monolito por capas, **sin microservicios**) con MySQL y JWT, hecho para reemplazar los mocks del frontend React de EP2.

Fuentes: `02_DSY1104_EP3_Requisitos_y_Rubrica_AVA.docx` (rúbrica oficial), `Infografía EP3_ Evaluación Parcial 3.png` (entregables y fechas) y la revisión de `frontend/src` (2026-11-06).

---

## Rúbrica e infografía (resumen)

**Alcance mínimo verificable**

1. Spring Boot conectado a **MySQL o PostgreSQL**, con entidades del caso, persistencia y reglas de negocio.
2. API REST con **CRUD** funcional y documentación en **Swagger/OpenAPI**.
3. React consumiendo los endpoints reales, con manejo de resultados, validaciones y errores.
4. **JWT** con roles y autorización efectiva de los recursos protegidos.
5. **Sesión persistente** al recargar y manejo de sesión vencida o inválida en el frontend.
6. Rutas, vistas y acciones restringidas por rol en el frontend, y endpoints sensibles protegidos **también** en el backend.

No hay que desplegar en la nube. Docker es opcional y no suma puntaje. La entrega se tiene que poder reproducir con instrucciones claras.

| Indicador (encargo grupal, 40%) | Peso | Dónde se cubre |
| --- | --- | --- |
| IE3.1.1 Backend con BD, lógica de negocio y modelos | 8% | 3, 5 |
| IE3.2.1 API REST: CRUD y Swagger *(sin niveles 80/30)* | 8% | 4, 5 |
| IE3.2.2 Integración frontend/backend por REST | 6% | 7 |
| IE3.3.1 JWT y roles en backend *(sin niveles 80/30)* | 6% | 6 |
| IE3.3.2 Gestión y persistencia de sesión en frontend | 6% | 7.2 |
| IE3.3.3 Restricciones de acceso en frontend | 6% | 7.2, 7.5, 7.6 |

**Defensa técnica individual (60%):** preguntas personalizadas sobre el código, los repositorios y la documentación entregada. Los temas son backend y modelos (12%), API/CRUD/Swagger (12%), flujo de integración (10%), JWT y roles (10%), sesión (8%) y control de acceso (8%). **Sin video, el máximo de cada indicador individual es 60%.**

**Evidencias que se verifican:** procedimiento reproducible (requisitos, `.env` de ejemplo, inicialización de la BD, credenciales demo), datos de prueba para leer, crear, editar y eliminar desde el frontend y/o Swagger, al menos **dos roles** con 401/403 correctos, sesión que se conserva al recargar, token expirado o inválido, y pruebas de integración documentadas.

**Entregables (la entrega en AVA es individual, aunque el trabajo sea grupal)**

- [ ] Enlace GitHub **público** del frontend.
- [ ] Enlace GitHub **público** del backend.
- [ ] Frontend comprimido (ZIP, sin `node_modules` ni `dist`).
- [ ] Backend comprimido (ZIP, sin `target`).
- [ ] Manual de usuario con pantallazos.
- [ ] Documento de integración.
- [ ] Video demostrativo.

**Fechas (sección 005D):** entrega el **viernes 27/11** (semana 16) y defensa el **miércoles 02/12** (semana 17).

---

## Decisiones

- **Monolito Spring Boot** en el puerto 8080, paquete `cl.losmaestros.ferreteria`, capas `controller / service / repository / model / dto / exception / config / security`. Sin microservicios ni gateway (confirmado).
- **MySQL 8** con `backend/docker-compose.yml` (o XAMPP). H2 solo en el perfil `test`.
- **Sin `/api/salud`**: era el ejemplo del taller y no lo usa el frontend.
- **El JSON habla el idioma del frontend**, para tocar lo menos posible en React:
  - producto: `id` (el código, ej. `MC001`), `nombre`, `categoria` (slug), `subcategoria`, `marca`, `unidad`, `precio`, `precioCompra`, `stock`, `stockMinimo`, `descripcion`, `image`, `estadoStock`;
  - roles en minúsculas (`admin`, `vendedor`, `cliente`), estados de usuario `Activo`/`Inactivo`, estados de pedido `En preparación`/`Enviado`/`Entregado`. Internamente son enums de Java, serializados con `@JsonValue` y `@JsonCreator`;
  - pedido: `id`, `cliente` (nombre), `fechaISO`, `estado`, `total`, `productos` (nombres) + `detalle` (líneas con código, nombre, precio, cantidad y subtotal).
- **Dos repos públicos** (lo pide la infografía): `ferreteria-los-maestros-frontend` (el repo actual renombrado, con su historial y la app en la raíz) y `ferreteria-los-maestros-backend` (nuevo). En local quedan como `proyecto-semestral/frontend` y `proyecto-semestral/backend`, cada uno con su propio git.

---

## 0. Qué necesita el frontend (revisión de `frontend/src`)

| Pantalla / archivo | Hoy usa | Necesita del backend |
| --- | --- | --- |
| `pages/Products.jsx` (catálogo) | `mocks/products.js`; filtra en el cliente por `q`, `categoria`[], `marca`[], `stock=1`, `min`, `max`, `orden` | `GET /api/productos` (lista completa de productos activos; los filtros pueden seguir en el cliente) |
| `components/products/FiltersSidebar.jsx` | `mocks/categories.js`, `mocks/brands.js` | `GET /api/categorias`, `GET /api/marcas` |
| `components/home/CategoriesSection.jsx` | imágenes en `assets/` + nombre desde el catálogo | `GET /api/categorias` (solo nombres; las imágenes siguen en el frontend) |
| `pages/ProductDetail.jsx` + `utils/catalog.js` | `getProductById`, relacionados de la misma categoría | `GET /api/productos/{id}` (404) y relacionados desde la lista o con `?categoria=` |
| `context/CartProvider.jsx` | catálogo mock (prop `catalog`) + `localStorage` `{id, cantidad}` | catálogo de `GET /api/productos`; el carrito sigue en `localStorage` |
| `components/cart/CartSummary.jsx` | botón "Continuar con la compra" **deshabilitado** | `POST /api/pedidos` (checkout del cliente) |
| `context/AuthProvider.jsx` + `LoginForm` | `mocks/users.js` (contraseñas en texto plano) + `localStorage` `auth_user` | `POST /api/auth/login` → JWT, `GET /api/auth/me` |
| `LoginForm` "Cuentas de prueba" | lista `users.js` con contraseñas | dejar las cuentas demo como texto fijo (o quitarlo); ya no hay mock |
| `ProtectedRoute`, `Navbar`, sidebars | `user.rol` de `localStorage` | rol desde `/api/auth/me`; el backend responde 401/403 |
| `pages/admin/Dashboard.jsx` | n° productos y usuarios del mock; "248 pedidos", "$4.892.360" y tendencias **inventadas** | `GET /api/admin/resumen` |
| `components/admin/RecentOrders.jsx` | `mocks/orders.js` | `GET /api/pedidos?limit=5` (los últimos) |
| `pages/admin/Products.jsx` | mock; alta solo valida y muestra un aviso; botones **Editar/Eliminar sin acción** | `GET/POST/PUT/DELETE /api/productos` |
| `AddProductForm` | categorías, marcas y unidades del mock; campo de imagen (archivo JPG/PNG/WEBP ≤ 2 MB) | `GET /api/categorias`, `/api/marcas`, `/api/productos/unidades`; imagen → `POST /api/productos/{id}/imagen` (mejora) |
| `utils/productValidation.js` | unicidad del código contra el mock | 409 del backend |
| `utils/stock.js` | umbral fijo `LOW_STOCK_THRESHOLD = 10` | `estadoStock` calculado con `stockMinimo` |
| `pages/admin/Users.jsx` | `mocks/adminUsers.js` (42 usuarios); alta solo muestra un aviso; botones **Editar/Eliminar sin acción** | `GET/POST/PUT/DELETE /api/usuarios` |
| `utils/userValidations.js` | unicidad del email contra el mock | 409 del backend |
| `pages/seller/Orders.jsx` | `mocks/orders.js`; cambio de estado y "Enviar" solo en estado React | `GET /api/pedidos`, `GET /api/pedidos/{id}`, `PATCH /api/pedidos/{id}/estado` |
| `components/contact/ContactForm.jsx` | `nombre`, `correo`, `telefono`, `asunto`, `mensaje`; solo muestra un aviso | `POST /api/contacto` (mejora) |
| `SellerSidebar` (Dashboard, Productos, Clientes… deshabilitados) | nada | no se implementan |

**Datos de los mocks que hay que completar al sembrar la BD**

- `products.js` (76 productos) no trae `precioCompra`, `stockMinimo` ni `descripcion`. En la semilla se usan `precioCompra ≈ 70%` del precio, `stockMinimo = 10` y una descripción corta.
- `orders.js` (8 pedidos) guarda el cliente como texto y productos que **no existen** en el catálogo ("Cemento Portland 25 kg"). Se reescriben con clientes reales de `adminUsers.js` (32 clientes) y productos reales del catálogo.
- `users.js` y `adminUsers.js` se unen en una sola tabla. Las 3 cuentas demo conservan sus contraseñas (`admin123`, `vendedor123`, `cliente123`) con BCrypt, y el resto recibe una contraseña demo común.

## 1. Endpoints

Todo bajo `http://localhost:8080/api`.

| Método y ruta | Acceso | Usado por | Prioridad |
| --- | --- | --- | --- |
| `POST /auth/login` | público | `AuthProvider.login` | obligatorio |
| `GET /auth/me` | autenticado | restaurar la sesión al recargar | obligatorio |
| `GET /categorias` | público | filtros, inicio, formulario | ✅ hecho |
| `GET /marcas` | público | filtros, datalist | ✅ hecho |
| `GET /productos` | público | catálogo, carrito (sin `precioCompra`) | ✅ hecho |
| `GET /productos/{id}` | público | detalle | ✅ hecho |
| `GET /unidades` | público | select de unidad | ✅ hecho |
| `GET /admin/productos` · `GET /admin/productos/{id}` | admin | tabla y edición (incluye `precioCompra`) | ✅ hecho |
| `POST /admin/productos` | admin | Agregar producto | ✅ hecho |
| `PUT /admin/productos/{id}` | admin | Editar producto | ✅ hecho |
| `DELETE /admin/productos/{id}` | admin | Eliminar producto | ✅ hecho |
| `GET /usuarios` | admin | tabla de usuarios | obligatorio |
| `GET /usuarios/{id}` | admin | editar | obligatorio |
| `POST /usuarios` | admin | Agregar usuario | obligatorio |
| `PUT /usuarios/{id}` | admin | Editar usuario | obligatorio |
| `DELETE /usuarios/{id}` | admin | Eliminar/desactivar | obligatorio |
| `GET /pedidos` | vendedor, admin | panel vendedor, pedidos recientes | obligatorio |
| `GET /pedidos/{id}` | vendedor, admin | modal de detalle | obligatorio |
| `PATCH /pedidos/{id}/estado` | vendedor | Cambiar estado / Enviar | obligatorio |
| `POST /pedidos` | cliente | checkout del carrito | recomendado (da sentido al rol cliente y es la mejor regla de negocio) |
| `GET /admin/resumen` | admin | indicadores del dashboard | recomendado |
| `GET /pedidos/mios` | cliente | "Mis pedidos" (pantalla nueva) | mejora |
| `POST /productos/{id}/imagen` | admin | campo imagen del formulario | mejora |
| `POST /contacto` · `GET /contacto` | público · admin | formulario de contacto | mejora |

**Decisión (catálogo):** el precio de compra es un dato interno, así que el catálogo público (`/api/productos`) no lo expone. La gestión completa vive en `/api/admin/productos`, que la seguridad protege con la regla `/api/admin/** → admin`. El cuerpo de POST/PUT usa los nombres del formulario (`codigo`, `precioVenta`…) para que cada error caiga en su campo; la respuesta usa los nombres que leen las vistas (`id`, `precio`, `image`).

**Modelo:** `Categoria` 1–N `Producto` · `Usuario` 1–N `Pedido` · `Pedido` 1–N `DetallePedido` N–1 `Producto` (+ `MensajeContacto` si se hace).

## 2. Preparación

- [x] Leer la rúbrica y la infografía y resumirlas aquí.
- [x] Arquitectura: monolito Spring Boot + MySQL (sin microservicios).
- [x] JDK 21 (`export JAVA_HOME=$(/usr/libexec/java_home -v 21)`), Maven Wrapper y Docker.
- [ ] Hacer commit de `backend/` (respaldo antes de seguir).
- [x] Sección 005D: entrega vie 27/11, defensa mié 02/12.
- [x] Separar en dos repos públicos: renombrar el actual a `ferreteria-los-maestros-frontend`, crear `ferreteria-los-maestros-backend`, reescribir el historial del frontend (app en la raíz, tag `ep2-monorepo` con el estado anterior) y ajustar el workflow de CI.
- [ ] Repartir responsables (sección 10).
- [ ] Postman/Insomnia o archivos `.http` para probar.

## 3. Proyecto base y base de datos (IE3.1.1)

- [x] Plantilla copiada a `backend/` y renombrada (`cl.losmaestros:ferreteria-backend`, paquete `cl.losmaestros.ferreteria`).
- [x] `mysql-connector-j` (runtime); H2 + `spring-boot-starter-webmvc-test` solo para tests.
- [x] `application.properties` con `DB_URL`, `DB_USER`, `DB_PASSWORD`, `CORS_ORIGINS` (lee `backend/.env`).
- [x] Perfil `test` con H2, `.env.example`, `docker-compose.yml` (MySQL 8.4), `.gitignore` con `.env`.
- [x] Swagger en `/swagger-ui.html` con título y descripción (`OpenApiConfig`).
- [x] CORS global para `http://localhost:5173` (`CorsConfig`; se mueve a `SecurityConfig` en la sección 6).
- [x] Quitar el `/api/salud` de la plantilla.
- [x] Entidades JPA del catálogo (`Categoria`, `Producto`). Faltan las de usuarios y pedidos.
- [ ] Entidades JPA con sus restricciones (`@Column(unique, nullable, length)`, relaciones `@ManyToOne` / `@OneToMany`).
- [x] `DatosIniciales` (`CommandLineRunner`) con `resources/datos/categorias.json` y `productos.json` (generados desde los mocks).
- [ ] Datos iniciales idempotentes (`CommandLineRunner` que solo inserta si la tabla está vacía) con los mocks completados (sección 0).
- [ ] `ddl-auto=update` en desarrollo; documentar cómo reiniciar la BD (`docker compose down -v`).

## 4. Convenciones de la API (IE3.2.1)

- [ ] DTOs de entrada y salida (`ProductoRequest/Response`, `UsuarioRequest/Response`, `PedidoResponse`, `LoginRequest/Response`…). Nunca exponer entidades ni `passwordHash`.
- [ ] Bean Validation en los `*Request`, con los mismos mensajes que el frontend (`productValidation.js`, `userValidations.js`).
- [ ] `@RestControllerAdvice` con JSON uniforme `{ status, error, mensaje, campos: { campo: "mensaje" } }`. Los nombres de `campos` deben coincidir con los `name` de los formularios (`codigo`, `precioVenta`, `email`…), para pintar el error en el campo correcto.
  - [ ] 400 validación o regla de negocio · 401 sin token o token inválido/expirado · 403 rol no permitido · 404 no existe · 409 duplicado o sin stock.
- [ ] 201 + cuerpo en POST, 200 en PUT/PATCH, 204 en DELETE.
- [ ] `@Tag`, `@Operation` y `@ApiResponse` en cada controller; esquema `bearerAuth` en Swagger (botón *Authorize*).

## 5. Módulos

### 5.1 Catálogo ✅ (2026-11-06, 18 tests: 8 unitarios + 9 de integración + arranque)

- [x] `Categoria`: `id` (slug), `nombre`.
- [x] `Producto`: `codigo` (PK, `^[A-Z]{2}\d{3}$`), `nombre` (3–80, con letra), `categoria` (FK), `subcategoria` (2–40), `marca` (2–30), `unidad`, `precioCompra` y `precio` (enteros 1–9.999.999), `stock` y `stockMinimo` (0–100.000), `descripcion` (≤ 120), `imagen` (URL), `activo`.
- [x] `GET /api/productos` → lista de productos activos (el catálogo actual tiene 76 y el frontend filtra y ordena en el cliente). Opcional: aceptar `?categoria=` para los relacionados.
- [x] `GET /api/productos/{id}` → 404 si no existe o está inactivo.
- [x] `GET /api/categorias`, `GET /api/marcas` y `GET /api/unidades` (`SELECT DISTINCT` de productos activos).
- [x] `GET /api/admin/productos` y `/{id}` (admin, con `precioCompra`).
- [x] `POST /api/admin/productos` (admin). Recibe los campos del formulario: `codigo`, `nombre`, `categoria`, `subcategoria`, `marca`, `unidad`, `precioCompra`, `precioVenta`, `stock`, `stockMinimo`, `descripcion`, `imagen` (URL opcional). **Regla:** `precio ≥ precioCompra` (400 en `precioVenta`); 409 en `codigo` si ya existe; 400 si la categoría no existe.
- [x] `PUT /api/admin/productos/{id}` (admin): mismas reglas; el código no se cambia.
- [x] `DELETE /api/admin/productos/{id}` (admin): soft delete (`activo=false`) para no romper pedidos antiguos → 204.
- [x] `estadoStock` en la respuesta: `Sin stock` si stock = 0, `Bajo stock` si stock ≤ stockMinimo, `En stock` si no.
- [x] Semilla: 9 categorías y 76 productos (las imágenes siguen en `frontend/public/products`; `image` guarda la ruta `/products/…`).

### 5.2 Usuarios y autenticación

- [ ] `Usuario`: `id`, `nombre` (3–60, nombre y apellido), `email` (único, en minúsculas, ≤ 80), `passwordHash` (BCrypt), `rol`, `estado`, `ultimoAcceso`, `creadoEn`.
- [ ] `POST /api/auth/login` `{ email, password }` → `{ token, expiraEn, usuario: { id, nombre, email, rol } }`. Credenciales malas → 401 "Correo o contraseña incorrectos". Usuario inactivo → 401/403 con mensaje. Actualiza `ultimoAcceso`.
- [ ] `GET /api/auth/me` → `{ id, nombre, email, rol }` del token.
- [ ] `GET /api/usuarios` → `[{ id, nombre, email, rol, estado, ultimoAcceso }]` (el frontend filtra y pagina en el cliente).
- [ ] `GET /api/usuarios/{id}` (404).
- [ ] `POST /api/usuarios` `{ nombre, email, rol, estado, password }`: contraseña de 8–64 caracteres con mayúscula, minúscula y número, sin espacios; 409 en `email` si ya existe → 201.
- [ ] `PUT /api/usuarios/{id}`: nombre, email, rol, estado y contraseña opcional.
- [ ] `DELETE /api/usuarios/{id}` → desactivar (`estado=Inactivo`) o borrar si no tiene pedidos. **Regla:** el admin no puede eliminarse, desactivarse ni quitarse el rol a sí mismo (400).
- [ ] Semilla: los 42 usuarios de `adminUsers.js` (3 admin, 8 vendedores, 32 clientes, 6 inactivos), con las 3 cuentas demo con sus contraseñas actuales.

### 5.3 Pedidos

- [ ] `Pedido`: `id`, `cliente` (FK `Usuario`), `fecha`, `estado`, `total`.
- [ ] `DetallePedido`: `pedido`, `producto` (FK), `nombreProducto` y `precioUnitario` copiados al comprar, `cantidad`, `subtotal`.
- [ ] `GET /api/pedidos` (vendedor/admin), ordenado por fecha descendente; `?limit=5` para `RecentOrders`. Respuesta con `id`, `cliente`, `fechaISO`, `estado`, `total` y `productos` (nombres), que es lo que ya pintan `OrdersTable` y `filterOrders`.
- [ ] `GET /api/pedidos/{id}` con `detalle`.
- [ ] `PATCH /api/pedidos/{id}/estado` `{ estado }` (vendedor). **Regla:** solo avanzar En preparación → Enviado → Entregado; cualquier otro cambio → 400. El select del modal debe mostrar solo el estado actual y el siguiente.
- [ ] **Checkout** `POST /api/pedidos` (cliente) `{ items: [{ id, cantidad }] }`, en una transacción (`@Transactional`): busca precio y stock en la BD (no confía en el frontend), 409 si no alcanza el stock (indicando qué producto), descuenta stock, calcula el total y responde 201 con el pedido.
- [ ] Semilla: unos 15 pedidos en distintos estados y fechas (últimos 90 días) con clientes y productos reales, para que funcionen los filtros de 7/30/90 días.

### 5.4 Dashboard admin

- [ ] `GET /api/admin/resumen` → `{ productos, productosBajoStock, usuarios, pedidosMes, ventasMes, variacionPedidos, variacionVentas }` (mes actual vs mes anterior). Reemplaza los números inventados.

### 5.5 Mejoras (solo si sobra tiempo)

- [ ] `GET /api/pedidos/mios` + página "Mis pedidos" para el cliente.
- [ ] `POST /api/productos/{id}/imagen` (multipart, JPG/PNG/WEBP ≤ 2 MB, guardado en disco y servido como estático).
- [ ] `MensajeContacto` + `POST /api/contacto` (público) y `GET /api/contacto` (admin).

## 6. Seguridad JWT y roles (IE3.3.1: hay que hacerlo completo)

- [ ] Dependencias: `spring-boot-starter-security` + `jjwt-api/impl/jackson`.
- [ ] Variables: `JWT_SECRET` (≥ 32 bytes) y `JWT_EXPIRATION_MINUTES` (ej. 60; usar 1–2 para demostrar la expiración en el video).
- [ ] `JwtService`: genera y valida el token (HS256, `sub` = email, claims `id` y `rol`, `iat`, `exp`).
- [ ] `JwtAuthFilter` (`OncePerRequestFilter`): lee `Authorization: Bearer …` y carga el `SecurityContext`. Si el token es inválido o expiró → 401 JSON con `mensaje: "Sesión expirada o inválida"`.
- [ ] `SecurityConfig`: `STATELESS`, CSRF desactivado, CORS desde la configuración, `BCryptPasswordEncoder`, `AuthenticationEntryPoint` (401) y `AccessDeniedHandler` (403) con el formato de error uniforme.
- [ ] Reglas:
  - [ ] Público: `POST /api/auth/login`, `GET /api/productos/**`, `GET /api/categorias`, `GET /api/marcas`, `GET /api/unidades`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`), `POST /api/contacto`.
  - [ ] Autenticado: `GET /api/auth/me`.
  - [ ] cliente: `POST /api/pedidos`, `GET /api/pedidos/mios`.
  - [ ] vendedor o admin: `GET /api/pedidos/**`; solo vendedor: `PATCH /api/pedidos/*/estado`.
  - [ ] admin: `/api/admin/**` (incluye productos y resumen), `/api/usuarios/**`, `GET /api/contacto`.
- [ ] Probar y dejar evidencia: sin token → 401, cliente en `/api/usuarios` → 403, vendedor en `POST /api/admin/productos` → 403, token expirado → 401.

## 7. Cambios en el frontend

### 7.1 Cliente HTTP (IE3.2.2)

- [ ] `frontend/.env.development` con `VITE_API_URL=http://localhost:8080/api` + `.env.example`.
- [ ] `src/services/api.js`: URL base, JSON, `Authorization: Bearer`, error con `{ status, mensaje, campos }`, y ante un 401 con sesión → `logout` + aviso "Tu sesión expiró".
- [ ] `authService`, `productService` (incluye categorías, marcas y unidades), `userService`, `orderService`, `adminService`.
- [ ] Estados de carga y error en cada pantalla (`PageLoader`, `Notice`).

### 7.2 Sesión y acceso (IE3.3.2 + IE3.3.3)

- [ ] `AuthProvider.login` → `POST /auth/login`; guardar `token` en `localStorage` (reemplaza `auth_user`).
- [ ] Al iniciar: si hay token, `GET /auth/me` → restaura `user`; si falla, limpia. Exponer `loading` en el contexto.
- [ ] `ProtectedRoute`: mientras `loading`, mostrar el loader (no redirigir antes de tiempo).
- [ ] Token vencido: revisar `exp` al cargar y responder al 401 del backend → `/login` con el mensaje "Tu sesión expiró, vuelve a iniciar sesión" y la ruta de retorno guardada.
- [ ] `logout` borra el token.
- [ ] Acciones por rol: botones de checkout solo para clientes (los demás ven "Inicia sesión como cliente"), acciones de admin y del vendedor dentro de sus paneles.
- [ ] `LoginForm`: reemplazar la lista generada desde `users.js` por las 3 cuentas demo escritas en el componente o en el README.
- [ ] Borrar `mocks/users.js`.

### 7.3 Catálogo, detalle y carrito

- [ ] Contexto o hook `useCatalog` que pide `GET /productos` y `GET /categorias` una vez; lo usan `Products`, `ProductDetail`, `CategoriesSection`, `CartProvider` y `utils/catalog.js`.
- [ ] `FiltersSidebar`: categorías y marcas del API.
- [ ] `ProductDetail`: 404 → "Producto no encontrado"; relacionados desde el catálogo cargado.
- [ ] `utils/stock.js`: usar `estadoStock` del backend.
- [ ] `CartSummary`: habilitar "Continuar con la compra" → si no hay sesión de cliente, ir a `/login`; si la hay, `POST /pedidos` → vaciar el carrito → confirmación con el n° de pedido; mostrar el 409 de stock.

### 7.4 Panel admin

- [ ] Dashboard: `GET /admin/resumen`; quitar los valores fijos y las tendencias inventadas.
- [ ] `RecentOrders`: `GET /pedidos?limit=5`.
- [ ] Productos: tabla desde `GET /admin/productos`; Agregar → `POST /admin/productos`; **Editar** (abre `AddProductForm` con los datos, el código bloqueado) → `PUT`; **Eliminar** (confirmación) → `DELETE`; refrescar la lista y mostrar los errores 400/409 por campo.
- [ ] `AddProductForm`: categorías, marcas y unidades del API. Si no se sube imagen, el producto queda con `image: null` → mostrar una imagen por defecto en `ProductCard` y en el detalle.
- [ ] Usuarios: tabla desde el API; Agregar → `POST` con la contraseña; **Editar** (contraseña opcional) → `PUT`; **Eliminar/Desactivar** → `DELETE`.
- [ ] `productValidation.js` / `userValidations.js`: quitar la unicidad contra los mocks (la valida el backend con 409).
- [ ] Borrar `mocks/products.js`, `categories.js`, `brands.js` y `adminUsers.js`.

### 7.5 Panel vendedor

- [ ] `Orders.jsx`: `GET /pedidos`; el modal de detalle usa `GET /pedidos/{id}`.
- [ ] `updateStatus` y `markAsSent` → `PATCH /pedidos/{id}/estado`; actualizar con la respuesta y mostrar el 400.
- [ ] Mostrar la fecha con `formatDateTime(fechaISO)` (el campo `fecha` ya formateado deja de existir).
- [ ] Borrar `mocks/orders.js`.

### 7.6 Tests del frontend

- [ ] Ajustar los tests que importan mocks (mockear los services con `vi.fn`).
- [ ] Nuevos: login ok/401, restaurar sesión con `/me`, sesión expirada, `ProtectedRoute` mientras carga y por rol, 409 en formularios.
- [ ] `npm run lint`, `npm test` y `npm run build` en verde.

## 8. Pruebas y documentación (entregables)

- [ ] Tests backend (perfil `test`, H2): services con JUnit 5 + Mockito (precio ≥ costo, 409 duplicados, transición de estados, stock en el checkout, admin que se borra a sí mismo) y controllers con MockMvc (validación 400, 401 sin token, 403 por rol).
- [ ] **Documento de integración** (`docs/documento-integracion.md`, entregable): arquitectura, flujo React → `api.js` → controller → service → repository → MySQL, tabla de endpoints con permisos, flujo del JWT y de la sesión, y la tabla de pruebas de integración (caso, pasos, esperado, obtenido, captura).
- [ ] **Manual de usuario con pantallazos** (entregable): cómo instalar y levantar, y paso a paso por rol (visitante, cliente, vendedor, admin).
- [ ] Colección Postman o `.http` con todos los endpoints y los casos 401/403.
- [ ] README raíz y `backend/README.md`: requisitos, `.env`, BD, orden de arranque (MySQL → backend → frontend), Swagger, cuentas demo.
- [ ] Actualizar `frontend/README.md` y `frontend/docs/` (ya no es "solo mock").
- [x] `.github/workflows/ci.yml` en el repo del backend: `./mvnw test` en cada push.
- [ ] ZIPs del frontend y del backend (sin `node_modules`, `dist`, `target` ni `.env`).

## 9. Video y defensa (60% individual)

- [ ] **Video demostrativo** (sin video, máximo 60%): Swagger (CRUD + Authorize), login con los 3 roles, CRUD de productos y usuarios desde el panel, el vendedor cambiando el estado, checkout del cliente, acceso prohibido (403 y redirección), recarga con la sesión conservada, token expirado.
- [ ] Cada integrante debe dominar **todo** el código: las preguntas son personalizadas sobre el código, los repos y los documentos.
- [ ] Repasar: capas y entidades, validaciones y reglas de negocio, códigos HTTP, cómo se genera y valida el JWT, `SecurityFilterChain`, cómo se restaura la sesión y cómo `ProtectedRoute` decide.

## 10. Orden de trabajo (entrega vie 27/11, defensa mié 02/12)

1. **Semana 14 (9–13/11):** catálogo completo en el backend (5.1) + `api.js` y catálogo, detalle y carrito en el frontend (7.1, 7.3 sin checkout).
2. **Semana 15 (16–20/11):** JWT y usuarios (5.2, 6) + sesión en el frontend (7.2) + CRUD admin (7.4).
3. **Semana 16 (23–27/11, antes de la entrega):** pedidos, checkout y resumen (5.3, 5.4, 7.5), tests, documento de integración, manual, README, ZIPs y video.
4. **Mié 02/12:** defensa.

Responsables: _por definir._
