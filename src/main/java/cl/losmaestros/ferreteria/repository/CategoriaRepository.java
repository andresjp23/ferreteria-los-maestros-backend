package cl.losmaestros.ferreteria.repository;

import cl.losmaestros.ferreteria.model.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {

    List<Categoria> findAllByOrderByNombreAsc();
}
