package cl.losmaestros.ferreteria.repository;

import cl.losmaestros.ferreteria.model.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // @EntityGraph trae el cliente y las líneas en la misma consulta (evita una consulta por pedido)

    @EntityGraph(attributePaths = {"cliente", "detalles"})
    List<Pedido> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = {"cliente", "detalles"})
    List<Pedido> findByClienteIdOrderByFechaDesc(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "detalles"})
    Optional<Pedido> findWithDetallesById(Long id);

    boolean existsByClienteId(Long clienteId);

    long countByFechaGreaterThanEqualAndFechaLessThan(LocalDateTime desde, LocalDateTime hasta);

    @Query("select coalesce(sum(p.total), 0) from Pedido p where p.fecha >= :desde and p.fecha < :hasta")
    long sumarTotal(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
