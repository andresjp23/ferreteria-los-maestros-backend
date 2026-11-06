package cl.losmaestros.ferreteria.repository;

import cl.losmaestros.ferreteria.model.Producto;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, String> {

    List<Producto> findByActivoTrueOrderByNombreAsc();

    List<Producto> findByActivoTrueAndCategoriaIdOrderByNombreAsc(String categoriaId);

    Optional<Producto> findByCodigoAndActivoTrue(String codigo);

    @Query("select distinct p.marca from Producto p where p.activo = true order by p.marca")
    List<String> findMarcasActivas();

    @Query("select distinct p.unidad from Producto p where p.activo = true order by p.unidad")
    List<String> findUnidadesActivas();

    /**
     * Productos activos con esos códigos, bloqueados hasta que termine la transacción (SELECT ... FOR UPDATE).
     * Así dos compras al mismo tiempo no pueden descontar el mismo stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.codigo in :codigos and p.activo = true")
    List<Producto> bloquearActivos(@Param("codigos") Collection<String> codigos);

    @Query("select count(p) from Producto p where p.activo = true and p.stock <= p.stockMinimo")
    long contarBajoStock();

    long countByActivoTrue();
}
