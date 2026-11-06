package cl.losmaestros.ferreteria.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración del catálogo: levantan la aplicación completa con H2
 * y los datos iniciales. Cada test se revierte al terminar (@Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogoApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String NUEVO = """
            {
              "codigo": "HE900", "nombre": "Martillo carpintero 16 oz", "categoria": "herramientas",
              "subcategoria": "Martillos", "marca": "Stanley", "unidad": "Unidad",
              "precioCompra": 6000, "precioVenta": 8990, "stock": 5, "stockMinimo": 10,
              "descripcion": "Mango de fibra de vidrio", "imagen": "/products/martillo.webp"
            }
            """;

    // ---------- Catálogo público ----------

    @Test
    void listaLosProductosSembradosSinPrecioDeCompra() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(76)))
                .andExpect(jsonPath("$[0].precioCompra").doesNotExist())
                .andExpect(jsonPath("$[0].estadoStock").exists());
    }

    @Test
    void filtraPorCategoria() throws Exception {
        mockMvc.perform(get("/api/productos").param("categoria", "materiales-construccion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].categoria", hasItem("materiales-construccion")));
    }

    @Test
    void detalleDeProducto() throws Exception {
        mockMvc.perform(get("/api/productos/MC001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("MC001"))
                .andExpect(jsonPath("$.nombre").value("Cemento Polpaico gris 25 kg"))
                .andExpect(jsonPath("$.categoriaNombre").value("Materiales de construcción"))
                .andExpect(jsonPath("$.image").value("/products/cemento.webp"))
                .andExpect(jsonPath("$.estadoStock").value("En stock"));
    }

    @Test
    void productoInexistenteResponde404ConFormatoDeError() throws Exception {
        mockMvc.perform(get("/api/productos/XX999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado"));
    }

    @Test
    void listasDeApoyo() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)));
        mockMvc.perform(get("/api/marcas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("Makita")));
        mockMvc.perform(get("/api/unidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("Saco")));
    }

    // ---------- CRUD del administrador ----------

    @Test
    void cicloCompletoCrearEditarEliminar() throws Exception {
        mockMvc.perform(post("/api/admin/productos").contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/productos/HE900"))
                .andExpect(jsonPath("$.precioCompra").value(6000))
                .andExpect(jsonPath("$.precio").value(8990))
                .andExpect(jsonPath("$.estadoStock").value("Bajo stock"));

        mockMvc.perform(put("/api/admin/productos/HE900").contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVO.replace("\"stock\": 5", "\"stock\": 50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.estadoStock").value("En stock"));

        mockMvc.perform(delete("/api/admin/productos/HE900"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/productos/HE900"))
                .andExpect(status().isNotFound());
    }

    @Test
    void codigoRepetidoResponde409EnElCampoCodigo() throws Exception {
        mockMvc.perform(post("/api/admin/productos").contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVO.replace("HE900", "MC001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.codigo").value("Ya existe un producto con este código"));
    }

    @Test
    void datosInvalidosResponden400ConErrorPorCampo() throws Exception {
        String invalido = """
                { "codigo": "123", "nombre": "", "categoria": "herramientas", "subcategoria": "Martillos",
                  "marca": "Stanley", "unidad": "Unidad", "precioCompra": 0, "precioVenta": 8990,
                  "stock": -1, "stockMinimo": 10 }
                """;

        mockMvc.perform(post("/api/admin/productos").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo")
                        .value("Formato inválido: 2 letras mayúsculas y 3 números (ej. MC001)"))
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.precioCompra").value("Debe ser mayor a 0"))
                .andExpect(jsonPath("$.campos.stock").value("No puede ser negativo"));
    }

    @Test
    void precioDeVentaBajoElCostoResponde400() throws Exception {
        mockMvc.perform(post("/api/admin/productos").contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVO.replace("\"precioVenta\": 8990", "\"precioVenta\": 5000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.precioVenta").value("No puede ser menor al precio de compra"));
    }
}
