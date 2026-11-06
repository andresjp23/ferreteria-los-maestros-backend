package cl.losmaestros.ferreteria.service;

import cl.losmaestros.ferreteria.dto.ResumenResponse;
import cl.losmaestros.ferreteria.repository.PedidoRepository;
import cl.losmaestros.ferreteria.repository.ProductoRepository;
import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indicadores del dashboard del administrador, calculados con los datos reales. */
@Service
@Transactional(readOnly = true)
public class ResumenService {

    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;

    public ResumenService(ProductoRepository productoRepository, UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public ResumenResponse resumen() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioMesSiguiente = inicioMes.plusMonths(1);
        LocalDateTime inicioMesAnterior = inicioMes.minusMonths(1);

        long pedidosMes = pedidoRepository.countByFechaGreaterThanEqualAndFechaLessThan(inicioMes, inicioMesSiguiente);
        long pedidosAnterior = pedidoRepository.countByFechaGreaterThanEqualAndFechaLessThan(inicioMesAnterior, inicioMes);
        long ventasMes = pedidoRepository.sumarTotal(inicioMes, inicioMesSiguiente);
        long ventasAnterior = pedidoRepository.sumarTotal(inicioMesAnterior, inicioMes);

        return new ResumenResponse(
                productoRepository.countByActivoTrue(),
                productoRepository.contarBajoStock(),
                usuarioRepository.count(),
                pedidosMes,
                ventasMes,
                variacion(pedidosMes, pedidosAnterior),
                variacion(ventasMes, ventasAnterior));
    }

    /** Variación porcentual con un decimal. Sin datos del mes anterior no hay con qué comparar: null. */
    static Double variacion(long actual, long anterior) {
        if (anterior == 0) return null;
        return Math.round((actual - anterior) * 1000.0 / anterior) / 10.0;
    }
}
