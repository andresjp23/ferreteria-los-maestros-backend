package cl.losmaestros.ferreteria.repository;

import cl.losmaestros.ferreteria.model.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductoRepository extends JpaRepository<Producto, String> {

    List<Producto> findByActivoTrueOrderByNombreAsc();

    List<Producto> findByActivoTrueAndCategoriaIdOrderByNombreAsc(String categoriaId);

    Optional<Producto> findByCodigoAndActivoTrue(String codigo);

    @Query("select distinct p.marca from Producto p where p.activo = true order by p.marca")
    List<String> findMarcasActivas();

    @Query("select distinct p.unidad from Producto p where p.activo = true order by p.unidad")
    List<String> findUnidadesActivas();
}
