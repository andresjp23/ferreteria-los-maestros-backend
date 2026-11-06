package cl.losmaestros.ferreteria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.losmaestros.ferreteria.dto.ProductoAdminResponse;
import cl.losmaestros.ferreteria.dto.ProductoRequest;
import cl.losmaestros.ferreteria.exception.ConflictoException;
import cl.losmaestros.ferreteria.exception.RecursoNoEncontradoException;
import cl.losmaestros.ferreteria.exception.ReglaNegocioException;
import cl.losmaestros.ferreteria.model.Categoria;
import cl.losmaestros.ferreteria.model.EstadoStock;
import cl.losmaestros.ferreteria.model.Producto;
import cl.losmaestros.ferreteria.repository.CategoriaRepository;
import cl.losmaestros.ferreteria.repository.ProductoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProductoService productoService;

    private final Categoria herramientas = new Categoria("herramientas", "Herramientas");

    private static ProductoRequest request(String codigo, int precioCompra, int precioVenta) {
        return new ProductoRequest(codigo, "Martillo carpintero", "herramientas", "Martillos", "Stanley",
                "Unidad", precioCompra, precioVenta, 20, 5, " Mango de fibra ", "");
    }

    @Test
    void crearGuardaElProductoConDatosNormalizados() {
        when(productoRepository.existsById("HE999")).thenReturn(false);
        when(categoriaRepository.findById("herramientas")).thenReturn(Optional.of(herramientas));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoAdminResponse creado = productoService.crear(request(" he999 ", 3000, 4990));

        assertThat(creado.id()).isEqualTo("HE999");
        assertThat(creado.precio()).isEqualTo(4990);
        assertThat(creado.precioCompra()).isEqualTo(3000);
        assertThat(creado.descripcion()).isEqualTo("Mango de fibra");
        assertThat(creado.image()).isNull();
        assertThat(creado.estadoStock()).isEqualTo(EstadoStock.EN_STOCK);
    }

    @Test
    void crearConCodigoRepetidoLanzaConflicto() {
        when(productoRepository.existsById("MC001")).thenReturn(true);

        assertThatThrownBy(() -> productoService.crear(request("MC001", 3000, 4990)))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe un producto con este código")
                .extracting("campo").isEqualTo("codigo");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void precioDeVentaMenorAlDeCompraNoSePermite() {
        when(productoRepository.existsById("HE999")).thenReturn(false);

        assertThatThrownBy(() -> productoService.crear(request("HE999", 5000, 4990)))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("campo").isEqualTo("precioVenta");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void categoriaInexistenteNoSePermite() {
        when(productoRepository.existsById("HE999")).thenReturn(false);
        when(categoriaRepository.findById("herramientas")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crear(request("HE999", 3000, 4990)))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("campo").isEqualTo("categoria");
    }

    @Test
    void actualizarNoPermiteCambiarElCodigo() {
        Producto existente = new Producto("HE001");
        when(productoRepository.findByCodigoAndActivoTrue("HE001")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> productoService.actualizar("HE001", request("HE002", 3000, 4990)))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("campo").isEqualTo("codigo");
    }

    @Test
    void eliminarDesactivaElProducto() {
        Producto existente = new Producto("HE001");
        when(productoRepository.findByCodigoAndActivoTrue("HE001")).thenReturn(Optional.of(existente));

        productoService.eliminar("HE001");

        assertThat(existente.isActivo()).isFalse();
    }

    @Test
    void obtenerProductoInexistenteLanza404() {
        when(productoRepository.findByCodigoAndActivoTrue("XX999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtener("xx999"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void estadoStockUsaElStockMinimoDelProducto() {
        assertThat(EstadoStock.calcular(0, 10)).isEqualTo(EstadoStock.SIN_STOCK);
        assertThat(EstadoStock.calcular(10, 10)).isEqualTo(EstadoStock.BAJO_STOCK);
        assertThat(EstadoStock.calcular(11, 10)).isEqualTo(EstadoStock.EN_STOCK);
        assertThat(EstadoStock.calcular(3, 2)).isEqualTo(EstadoStock.EN_STOCK);
    }
}
