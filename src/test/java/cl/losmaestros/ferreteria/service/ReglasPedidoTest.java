package cl.losmaestros.ferreteria.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.losmaestros.ferreteria.model.EstadoPedido;
import org.junit.jupiter.api.Test;

/** Reglas puras de pedidos y del dashboard (sin base de datos). */
class ReglasPedidoTest {

    @Test
    void cadaEstadoTieneUnSoloSiguiente() {
        assertThat(EstadoPedido.EN_PREPARACION.siguiente()).contains(EstadoPedido.ENVIADO);
        assertThat(EstadoPedido.ENVIADO.siguiente()).contains(EstadoPedido.ENTREGADO);
        assertThat(EstadoPedido.ENTREGADO.siguiente()).isEmpty();
    }

    @Test
    void estadoDesdeElTextoDelFrontend() {
        assertThat(EstadoPedido.desde("En preparación")).contains(EstadoPedido.EN_PREPARACION);
        assertThat(EstadoPedido.desde("ENVIADO")).isEmpty();
    }

    @Test
    void variacionPorcentualConUnDecimal() {
        assertThat(ResumenService.variacion(120, 100)).isEqualTo(20.0);
        assertThat(ResumenService.variacion(2, 3)).isEqualTo(-33.3);
        assertThat(ResumenService.variacion(5, 0)).isNull();
    }
}
