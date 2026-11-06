package cl.losmaestros.ferreteria.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.losmaestros.ferreteria.model.EstadoPedido;
import cl.losmaestros.ferreteria.model.Pedido;
import cl.losmaestros.ferreteria.repository.PedidoRepository;
import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Pedidos: listado del vendedor, cambio de estado, checkout del cliente y resumen del admin. */
class PedidoApiTest extends ApiTestBase {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Long pedidoEn(EstadoPedido estado) {
        return pedidoRepository.findAll().stream()
                .filter(p -> p.getEstado() == estado)
                .map(Pedido::getId)
                .findFirst().orElseThrow();
    }

    private ResultActions cambiarEstado(Long id, String estado, String token) throws Exception {
        return mockMvc.perform(patch("/api/pedidos/" + id + "/estado").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"" + estado + "\"}"));
    }

    private ResultActions comprar(String items) throws Exception {
        return mockMvc.perform(post("/api/pedidos").header("Authorization", bearer(CLIENTE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":" + items + "}"));
    }

    private int stockDe(String codigo) throws Exception {
        String body = mockMvc.perform(get("/api/productos/" + codigo)).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.stock");
    }

    // ---------- Vendedor ----------

    @Test
    void elVendedorVeTodosLosPedidosConElFormatoDelPanel() throws Exception {
        mockMvc.perform(get("/api/pedidos").header("Authorization", bearer(VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)))
                .andExpect(jsonPath("$[0].cliente").isString())
                .andExpect(jsonPath("$[0].fechaISO").isString())
                .andExpect(jsonPath("$[0].estado").isString())
                .andExpect(jsonPath("$[0].productos").isArray())
                .andExpect(jsonPath("$[0].detalle[0].precioUnitario").isNumber());
    }

    @Test
    void limitDevuelveLosMasRecientes() throws Exception {
        mockMvc.perform(get("/api/pedidos").param("limit", "5").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void detalleYPedidoInexistente() throws Exception {
        String vendedor = bearer(VENDEDOR);
        Long id = pedidoEn(EstadoPedido.ENTREGADO);
        mockMvc.perform(get("/api/pedidos/" + id).header("Authorization", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("Entregado"));
        mockMvc.perform(get("/api/pedidos/999999").header("Authorization", vendedor))
                .andExpect(status().isNotFound());
    }

    @Test
    void elClienteNoVeLosPedidosDeTodos() throws Exception {
        mockMvc.perform(get("/api/pedidos").header("Authorization", bearer(CLIENTE)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void elEstadoAvanzaUnPasoALaVez() throws Exception {
        String vendedor = bearer(VENDEDOR);
        Long id = pedidoEn(EstadoPedido.EN_PREPARACION);

        cambiarEstado(id, "Entregado", vendedor)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.estado")
                        .value("Un pedido \"En preparación\" solo puede pasar a \"Enviado\""));

        cambiarEstado(id, "Enviado", vendedor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Enviado"));

        cambiarEstado(id, "En preparación", vendedor).andExpect(status().isBadRequest());

        cambiarEstado(id, "Entregado", vendedor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Entregado"));

        cambiarEstado(id, "Entregado", vendedor)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.estado").value("El pedido ya fue entregado"));
    }

    @Test
    void soloElVendedorCambiaEstados() throws Exception {
        Long id = pedidoEn(EstadoPedido.EN_PREPARACION);
        cambiarEstado(id, "Enviado", bearer(ADMIN)).andExpect(status().isForbidden());
        cambiarEstado(id, "Enviado", bearer(CLIENTE)).andExpect(status().isForbidden());
    }

    // ---------- Checkout del cliente ----------

    @Test
    void checkoutCalculaElTotalEnElServidorYDescuentaStock() throws Exception {
        int stockAntes = stockDe("MC001");

        // MC001 cuesta $5.990 y viene dos veces: se juntan en una línea de 3 unidades
        comprar("[{\"id\":\"MC001\",\"cantidad\":2},{\"id\":\"mc001\",\"cantidad\":1}]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("En preparación"))
                .andExpect(jsonPath("$.cliente").value("Camila Rojas"))
                .andExpect(jsonPath("$.detalle", hasSize(1)))
                .andExpect(jsonPath("$.detalle[0].cantidad").value(3))
                .andExpect(jsonPath("$.total").value(3 * 5990));

        org.junit.jupiter.api.Assertions.assertEquals(stockAntes - 3, stockDe("MC001"));

        mockMvc.perform(get("/api/pedidos/mios").header("Authorization", bearer(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(3 * 5990));
    }

    @Test
    void sinStockSuficienteNoSeCreaNadaYResponde409() throws Exception {
        int stockAntes = stockDe("MC001");
        long pedidosAntes = pedidoRepository.count();

        comprar("[{\"id\":\"HE001\",\"cantidad\":1},{\"id\":\"MC001\",\"cantidad\":1000}]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "Stock insuficiente para \"Cemento Polpaico gris 25 kg\": quedan " + stockAntes + " unidades"));

        org.junit.jupiter.api.Assertions.assertEquals(stockAntes, stockDe("MC001"));
        org.junit.jupiter.api.Assertions.assertEquals(pedidosAntes, pedidoRepository.count());
    }

    @Test
    void productoInexistenteOCarritoVacio() throws Exception {
        comprar("[{\"id\":\"XX999\",\"cantidad\":1}]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El producto XX999 ya no está disponible"));
        comprar("[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.items").value("El carrito está vacío"));
        comprar("[{\"id\":\"MC001\",\"cantidad\":0}]")
                .andExpect(status().isBadRequest());
    }

    @Test
    void soloLosClientesCompran() throws Exception {
        mockMvc.perform(post("/api/pedidos").header("Authorization", bearer(VENDEDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"id\":\"MC001\",\"cantidad\":1}]}"))
                .andExpect(status().isForbidden());
    }

    // ---------- Relación con usuarios y dashboard ----------

    @Test
    void noSeEliminaUnUsuarioConPedidos() throws Exception {
        Long id = usuarioRepository.findByEmail(CLIENTE).orElseThrow().getId();
        mockMvc.perform(delete("/api/usuarios/" + id).header("Authorization", bearer(ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El usuario tiene pedidos. Desactívalo en vez de eliminarlo"));
    }

    @Test
    void resumenDelDashboardSoloAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/resumen").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos").value(76))
                .andExpect(jsonPath("$.usuarios").value(42))
                .andExpect(jsonPath("$.productosBajoStock").isNumber())
                .andExpect(jsonPath("$.pedidosMes").isNumber())
                .andExpect(jsonPath("$.ventasMes").isNumber());
        mockMvc.perform(get("/api/admin/resumen").header("Authorization", bearer(VENDEDOR)))
                .andExpect(status().isForbidden());
    }
}
