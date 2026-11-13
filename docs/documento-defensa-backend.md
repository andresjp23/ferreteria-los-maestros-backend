# Ferretería Los Maestros — Backend

## Documento de apoyo para la defensa técnica (EP3)

Este documento explica cómo está construido el backend y dónde está cada parte en el código, organizado según los indicadores de la defensa técnica. Complementa a:

- el [documento de integración](documento-integracion.md): arquitectura, diagramas, endpoints, manejo de errores y pruebas P01–P23 con capturas;
- el [documento del frontend](https://github.com/andresjp23/ferreteria-los-maestros-frontend/blob/main/docs/documento-ferreteria-los-maestros.md): flujo de integración, sesión y control de acceso del lado de React;
- el [manual de usuario](https://github.com/andresjp23/ferreteria-los-maestros-frontend/blob/main/docs/manual-usuario.md).

Las referencias `archivo:línea` son relativas a `src/main/java/cl/losmaestros/ferreteria/`.

## Índice

1. [Resumen del backend](#1-resumen-del-backend)
2. [IE3.1.2 · Backend, persistencia, modelos y lógica de negocio](#2-ie312--backend-persistencia-modelos-y-lógica-de-negocio)
3. [IE3.2.3 · API REST, CRUD y Swagger](#3-ie323--api-rest-crud-y-swagger)
4. [IE3.2.4 · Flujo de integración (lado del servidor)](#4-ie324--flujo-de-integración-lado-del-servidor)
5. [IE3.3.4 · JWT, roles y autorización](#5-ie334--jwt-roles-y-autorización)
6. [IE3.3.5 · Sesión (lado del servidor)](#6-ie335--sesión-lado-del-servidor)
7. [IE3.3.6 · Por qué el backend valida los permisos](#7-ie336--por-qué-el-backend-valida-los-permisos)
8. [Tests](#8-tests)
9. [Decisiones, limitaciones y mejoras](#9-decisiones-limitaciones-y-mejoras)
10. [Preparación antes de la defensa](#10-preparación-antes-de-la-defensa)

---

## 1. Resumen del backend

- **Spring Boot 4.1 con Java 21**, en un solo proyecto (monolito por capas) que corre en el puerto 8080.
- **MySQL 8.4** en Docker. Hibernate crea y actualiza las tablas (`ddl-auto=update`) y `DatosIniciales` carga los datos de ejemplo solo si las tablas están vacías.
- **Spring Security + JWT** (jjwt, HS256), sin sesiones en el servidor (`STATELESS`). Contraseñas con BCrypt.
- **Swagger UI** en <http://localhost:8080/swagger-ui.html>, generado por springdoc a partir de las anotaciones de los controladores.

Recorrido de una petición:

```
Petición HTTP → JwtAuthFilter → SecurityConfig (¿rol permitido?) → Controller (@Valid DTO)
             → Service (reglas de negocio, @Transactional) → Repository (JPA) → MySQL
             ← DTO de respuesta en JSON  ←  GlobalExceptionHandler si algo falla
```

---

## 2. IE3.1.2 · Backend, persistencia, modelos y lógica de negocio

**Qué piden:** identificar una entidad, su repositorio, un servicio y el controlador que la expone, y explicar el recorrido hasta la base de datos.

### 2.1 Ejemplo recomendado: Producto

| Capa | Archivo | Qué mostrar |
| --- | --- | --- |
| **Entidad** | `model/Producto.java:14` | `@Entity` + `@Table(name = "productos")`. La clave es el código (`@Id`, `:18`). La categoría es `@ManyToOne` (`:25`), es decir, una clave foránea `categoria_id`. El campo `activo` permite el borrado lógico |
| **Repositorio** | `repository/ProductoRepository.java:13` | `extends JpaRepository<Producto, String>`: Spring genera `save`, `findById`, `findAll`… sin escribir SQL. Agrega consultas propias con `@Query`, como `bloquearActivos` (`:31-33`) |
| **Servicio** | `service/ProductoService.java` | `crear` (`:66`): revisa que el código no exista (409) y aplica la regla de precios (`:107`, 400). `eliminar` (`:93`): `setActivo(false)` |
| **Controlador** | `controller/AdminProductoController.java` | `@RequestMapping("/api/admin/productos")` (`:27`). `@PostMapping` (`:51`) recibe `@Valid @RequestBody ProductoRequest` y responde **201 Created** (`:58-61`) |
| **DTO** | `dto/ProductoRequest.java:15` | `record` con Bean Validation: `@NotBlank`, `@Pattern` (código `AA000`), `@Min`, `@Max`, `@Size` |

**Cómo explicar el recorrido hasta la BD:** el controlador valida el formato del JSON con `@Valid` y le pasa el DTO al servicio. El servicio aplica las reglas que dependen de los datos (código repetido, precio de venta ≥ precio de compra), arma la entidad y llama a `productoRepository.save()`. Hibernate traduce eso en un `INSERT INTO productos …` dentro de la transacción que abre `@Transactional`. Si algo falla antes del commit, no se guarda nada.

### 2.2 Modelo de datos

| Entidad | Tabla | Relaciones |
| --- | --- | --- |
| `Categoria` | `categorias` | Una categoría tiene muchos productos |
| `Producto` | `productos` | `@ManyToOne` a `Categoria` |
| `Usuario` | `usuarios` | Rol (`ADMIN`, `VENDEDOR`, `CLIENTE`) y estado (`ACTIVO`, `INACTIVO`) como enums |
| `Pedido` | `pedidos` | `@ManyToOne` a `Usuario` (el cliente); `@OneToMany` a sus detalles con `cascade = ALL` (`model/Pedido.java:44`) |
| `DetallePedido` | `detalles_pedido` | `@ManyToOne` a `Pedido` y a `Producto`. Guarda el **nombre y el precio al momento de la compra** |

El diagrama entidad-relación está en el [documento de integración, sección 2](documento-integracion.md#2-arquitectura).

**Decisiones que conviene saber justificar:**
- **El detalle copia nombre y precio:** si mañana sube el precio del cemento, los pedidos antiguos siguen mostrando lo que se pagó.
- **Borrado lógico de productos** (`activo = false`): borrar la fila rompería los detalles de pedidos que la referencian.
- **`Pedido.agregarDetalle`** (`model/Pedido.java:58`) suma el subtotal al total dentro de la propia entidad, así el total nunca queda desalineado con sus líneas.

### 2.3 Lógica de negocio: el checkout

`service/PedidoService.java:65` (`crear`) es la regla más importante:

1. Junta cantidades si el mismo producto viene dos veces (`:70-72`).
2. **Bloquea los productos** con `bloquearActivos` → `SELECT … FOR UPDATE` (`:75`). Si dos clientes compran el último taladro al mismo tiempo, el segundo espera a que termine el primero y luego ve el stock real.
3. Por cada producto: si no existe o no hay stock suficiente, lanza `ConflictoException` (409). Si alcanza, descuenta el stock y agrega la línea (`:79-90`).
4. Guarda el pedido. Todo ocurre en **una transacción** (`@Transactional`, `:64`): si falla una línea, se revierte todo y no queda stock descontado a medias.

El precio y el total **los calcula el servidor**. El navegador solo envía `id` y `cantidad`.

**Otra regla, el estado del pedido:** `PedidoService.cambiarEstado` (`:97`) usa `EstadoPedido.siguiente()` (`model/EstadoPedido.java:25`) para permitir solo el paso siguiente: En preparación → Enviado → Entregado. Cualquier otro cambio responde 400.

Otras reglas (correo único, no eliminar la propia cuenta, no eliminar usuarios con pedidos) están en la tabla de la [sección 3 del documento de integración](documento-integracion.md#3-componentes-del-backend).

**Preguntas probables:**
- *¿Por qué `@Transactional(readOnly = true)` en la clase y `@Transactional` en los métodos que escriben?* Las lecturas no necesitan bloquear ni hacer *flush*; las escrituras sí necesitan su transacción.
- *¿Qué pasa si se reinicia el backend?* Los datos siguen en MySQL (volumen de Docker). `DatosIniciales` no duplica nada porque solo carga si las tablas están vacías.
- *¿Por qué DTOs y no devolver la entidad?* Para no exponer `passwordHash` ni `precioCompra` donde no corresponde, y para que el JSON tenga los nombres que usa el frontend.

---

## 3. IE3.2.3 · API REST, CRUD y Swagger

**Qué piden:** abrir un endpoint en Swagger y explicar el método HTTP, la entrada, la respuesta y el manejo de errores.

### 3.1 CRUD de productos

| Operación | Método y ruta | Respuesta correcta | Errores |
| --- | --- | --- | --- |
| Listar | `GET /api/admin/productos` | 200 + lista | 401, 403 |
| Obtener | `GET /api/admin/productos/{codigo}` | 200 | 404 |
| Crear | `POST /api/admin/productos` | **201 Created** + header `Location` | 400, 409 |
| Editar | `PUT /api/admin/productos/{codigo}` | 200 | 400, 404 |
| Eliminar | `DELETE /api/admin/productos/{codigo}` | **204 No Content** | 404 |

También hay CRUD de usuarios (`/api/usuarios`) y la creación y el cambio de estado de pedidos (`/api/pedidos`). La tabla completa está en el [documento de integración, sección 6](documento-integracion.md#6-endpoints-y-permisos).

### 3.2 Demostración en Swagger: `POST /api/admin/productos`

1. `POST /api/auth/login` con `admin@losmaestros.cl` / `admin123` → copiar el `token`.
2. Botón **Authorize** → pegar el token. Swagger lo envía como `Authorization: Bearer …`. Esto está configurado en `config/OpenApiConfig.java:26-30`.
3. Abrir `POST /api/admin/productos` y explicar:
   - **Método POST**, porque crea un recurso nuevo.
   - **Entrada:** JSON con `codigo`, `nombre`, `categoria`, precios, stock… validado por `ProductoRequest`.
   - **Respuesta 201** con el producto creado y el header `Location: /api/admin/productos/{codigo}`.
   - **Errores documentados** con `@ApiResponse` en el controlador (`AdminProductoController.java:52-57`): 400 si los datos son inválidos o el precio de venta es menor, 409 si el código ya existe.
4. Mostrar los errores en vivo: el mismo código otra vez → **409** (P12); precio de venta menor → **400** con `campos.precioVenta` (P13); sin token → **401**; con el token del vendedor → **403** (P15).

### 3.3 Manejo de errores

`exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`) convierte cada excepción en un JSON con el mismo formato:

```json
{ "status": 409, "error": "Conflict", "mensaje": "Ya existe un producto con este código",
  "campos": { "codigo": "Ya existe un producto con este código" } }
```

| Excepción | Código | Handler |
| --- | --- | --- |
| `MethodArgumentNotValidException` (falla `@Valid`) | 400 | `:33` |
| `ReglaNegocioException` | 400 | `:57` |
| `NoAutorizadoException` (login incorrecto) | 401 | `:47` |
| `AccesoDenegadoException` (cuenta inactiva) | 403 | `:52` |
| `RecursoNoEncontradoException` | 404 | `:67` |
| `ConflictoException` | 409 | `:62` |
| Cualquier otra | 500, sin mostrar detalles internos | `:93` |

Los 401 y 403 de seguridad (sin token o sin rol) los responde `security/RestAuthHandlers.java` con el mismo formato.

**Preguntas probables:**
- *¿Por qué PUT y no PATCH para editar productos?* El formulario envía el producto completo. PATCH se usa en `/pedidos/{id}/estado`, que cambia un solo campo.
- *¿Diferencia entre 400 y 409?* 400: el dato está mal formado o rompe una regla. 409: el dato es válido, pero choca con el estado actual de la BD (código repetido, sin stock).
- *¿Por qué `campos`?* Para que el frontend muestre cada error debajo de su input.

---

## 4. IE3.2.4 · Flujo de integración (lado del servidor)

El recorrido completo, desde el clic en React, está en el [documento del frontend, sección 9.1](https://github.com/andresjp23/ferreteria-los-maestros-frontend/blob/main/docs/documento-ferreteria-los-maestros.md#91-ie324--flujo-de-integración-frontendbackend), y en el diagrama de secuencia del [documento de integración, sección 5](documento-integracion.md#5-flujo-de-una-petición-rest-el-checkout). Del lado del servidor:

| Paso | Dónde |
| --- | --- |
| CORS permite el origen `http://localhost:5173` | `config/SecurityConfig.java` (bean CORS, lee `CORS_ORIGINS`) |
| El filtro autentica el token | `security/JwtAuthFilter.java:43` |
| La regla de seguridad exige el rol `CLIENTE` | `config/SecurityConfig.java:55` |
| El controlador recibe el usuario autenticado y el carrito | `controller/PedidoController.java:75-77` |
| El servicio crea el pedido en una transacción | `service/PedidoService.java:65` |
| Responde 201 + `PedidoResponse` en JSON | `PedidoController.java:78` |

**Detalle importante:** el id del cliente **no viene en el JSON**. Sale del token, mediante `@AuthenticationPrincipal UsuarioAutenticado usuario`. Así, un cliente no puede crear pedidos a nombre de otro.

---

## 5. IE3.3.4 · JWT, roles y autorización

**Qué piden:** dónde se crea, se valida y se usa el JWT, y cómo se verifica el rol en el backend.

| Qué | Dónde | Detalle |
| --- | --- | --- |
| **Login** | `service/AuthService.java:38` | Busca por correo, compara la contraseña con BCrypt y revisa que la cuenta esté activa. Error → 401 "Correo o contraseña incorrectos" |
| **Se crea** | `security/JwtService.java:35` (`generar`) | `sub` = id del usuario, `email`, `rol`, `iat` y `exp`. Firmado con **HS256** usando `JWT_SECRET` (nunca en el repositorio) |
| **Se valida** | `security/JwtService.java:55` (`validar`) | Verifica la firma y el vencimiento. Lanza `ExpiredJwtException` o `JwtException` |
| **Se usa en cada petición** | `security/JwtAuthFilter.java:43` | Lee `Authorization: Bearer …`, valida, **busca al usuario en la BD** y lo registra en el `SecurityContext` con su autoridad `ROLE_…` (`:70`, `model/Rol.java:25`) |
| **Se verifica el rol** | `config/SecurityConfig.java:46-62` | Reglas `requestMatchers(...).hasRole(...)` por ruta y método |
| **Respuestas 401/403** | `security/RestAuthHandlers.java:31` y `:39` | Sin usuario autenticado → 401 con el motivo ("Tu sesión expiró…"). Rol insuficiente → 403 |

Reglas de `SecurityConfig.java`:

```java
.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()                // :50
.requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias", ...).permitAll()
.requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CLIENTE")            // :55
.requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado").hasRole("VENDEDOR") // :57
.requestMatchers("/api/admin/**", "/api/usuarios/**").hasRole("ADMIN")          // :62
.anyRequest().authenticated()
```

**Por qué el rol se toma de la BD y no del token:** si el admin desactiva una cuenta o le cambia el rol, el cambio aplica en la siguiente petición, aunque el token siga vigente. Es una consulta por petición a cambio de no poder seguir usando un token "viejo".

**Demostración:** en Swagger, `GET /api/usuarios` sin token → 401 (P08); con token de cliente → 403 (P09); con token de admin → 200 (P10). Para el token alterado: cambiar un carácter → 401 "Sesión inválida" (P07).

**Preguntas probables:**
- *¿Qué es HS256?* Firma HMAC con SHA-256 y una clave secreta compartida. Quien no tiene la clave no puede crear ni modificar un token válido. El contenido no está cifrado (es Base64), por eso el token no lleva datos sensibles.
- *¿Dónde está la clave?* En la variable de entorno `JWT_SECRET` (archivo `.env`, ignorado por git). Debe tener al menos 32 caracteres para HS256.
- *¿Qué pasa si alguien cambia `"rol":"cliente"` por `"admin"` en el token?* La firma deja de coincidir y `validar` lanza `JwtException` → 401.

---

## 6. IE3.3.5 · Sesión (lado del servidor)

- **Sin estado en el servidor:** `sessionCreationPolicy(STATELESS)` (`config/SecurityConfig.java:38`). El backend no guarda sesiones: toda la información viaja en el token, y por eso cualquier petición se puede atender sin "recordar" al usuario.
- **Restaurar la sesión al recargar:** el frontend llama a `GET /api/auth/me` (`controller/AuthController.java:48`), que devuelve el usuario del token, leído desde la BD (`AuthService.usuarioActual`, `:56`). Prueba P06.
- **Vencimiento:** `exp` = ahora + `JWT_EXPIRATION_MINUTES` (60 por defecto). Con un token vencido, el filtro marca el motivo (`JwtAuthFilter.java:60-61`) y `RestAuthHandlers` responde 401 "Tu sesión expiró. Inicia sesión nuevamente". El frontend recibe ese 401 y cierra la sesión.
- **Demostración de expiración:** levantar con `JWT_EXPIRATION_MINUTES=1`, iniciar sesión y esperar un minuto.

El manejo de la sesión en React (localStorage, loader, `setTimeout`) está en el [documento del frontend, sección 9.3](https://github.com/andresjp23/ferreteria-los-maestros-frontend/blob/main/docs/documento-ferreteria-los-maestros.md#93-ie335--gestión-y-persistencia-de-sesión).

---

## 7. IE3.3.6 · Por qué el backend valida los permisos

El frontend oculta botones y protege rutas, pero corre en el navegador del usuario: se puede modificar, o se puede saltar por completo llamando a la API con Swagger, Postman o curl. Por eso **cada endpoint protegido revisa el token y el rol en `SecurityConfig`**, y algunas reglas por usuario también se revisan en el servicio:

| Regla | Dónde |
| --- | --- |
| Solo el cliente puede comprar, y siempre a su propio nombre (id tomado del token) | `SecurityConfig.java:55`, `PedidoController.java:75-77` |
| El admin no puede eliminarse, desactivarse ni quitarse el rol de admin | `service/UsuarioService.java:66` y `:92` (reciben `idActual`) |
| Un usuario desactivado deja de poder usar su token | `JwtAuthFilter.java:52-53` |

Las pruebas P09, P15, P19 y P22 muestran el 403 con un token válido del rol equivocado.

---

## 8. Tests

```bash
./mvnw test    # perfil "test" con H2 en memoria; no necesita MySQL
```

**55 tests** en `src/test/java/cl/losmaestros/ferreteria/`:

| Clase | Tests | Qué prueba |
| --- | --- | --- |
| `controller/AuthApiTest` | 9 | Login correcto e incorrecto, campos vacíos, cuenta inactiva, `/auth/me`, token alterado o vencido, usuario desactivado con token vigente |
| `controller/AutorizacionApiTest` | 4 | 401 sin token y 403 con el rol equivocado |
| `controller/CatalogoApiTest` | 9 | Catálogo público, 404 y CRUD de productos del admin |
| `controller/PedidoApiTest` | 12 | Checkout, stock insuficiente, cambio de estado y permisos |
| `controller/UsuarioApiTest` | 9 | CRUD de usuarios y reglas sobre la propia cuenta |
| `service/ProductoServiceTest` | 8 | Reglas de producto con Mockito |
| `service/ReglasPedidoTest` | 3 | Avance de estados del pedido |
| `FerreteriaApplicationTests` | 1 | El contexto de Spring levanta |

Los tests de `controller/` usan **MockMvc**: envían peticiones HTTP reales a la aplicación completa (filtros de seguridad incluidos) sin abrir un puerto.

---

## 9. Decisiones, limitaciones y mejoras

| Tema | Decisión | Por qué / alternativa |
| --- | --- | --- |
| Monolito por capas | Un solo proyecto Spring Boot | El alcance cabe en una aplicación. Microservicios agregarían un gateway, varias BD y comunicación entre servicios sin beneficio para el caso |
| MySQL en Docker | `docker compose up -d` | Misma versión para cualquiera que levante el proyecto; XAMPP también sirve |
| `ddl-auto=update` | Hibernate crea las tablas | Rápido para desarrollo. En producción convendría Flyway o Liquibase con migraciones versionadas |
| Bloqueo pesimista en el checkout | `SELECT … FOR UPDATE` | Evita vender dos veces el último producto. La alternativa es el bloqueo optimista con `@Version` y reintentos |
| Rol leído de la BD en cada petición | Una consulta extra | A cambio, desactivar una cuenta o cambiarle el rol aplica de inmediato |

Las limitaciones conocidas (código de producto eliminado no reutilizable, JWT en `localStorage`, sin *refresh token*, sin revocación de tokens, imagen como ruta, solo HTTP local) están explicadas con su mejora en el [documento de integración, sección 11](documento-integracion.md#11-limitaciones-y-mejoras-posibles).

---

## 10. Preparación antes de la defensa

- Levantar todo desde cero siguiendo el [README](../README.md#puesta-en-marcha) y comprobar que aparece `Datos iniciales: 42 usuarios` (o que el catálogo carga).
- Tener abiertos `SecurityConfig.java`, `JwtService.java`, `JwtAuthFilter.java`, `PedidoService.java` y `AdminProductoController.java`.
- Tener Swagger abierto, con un token de admin y otro de cliente a mano, sin mostrarlos completos en pantalla.
- Saber explicar con palabras propias: entidad → repositorio → servicio → controlador; qué contiene el JWT y quién lo firma; por qué 401 y por qué 403.
