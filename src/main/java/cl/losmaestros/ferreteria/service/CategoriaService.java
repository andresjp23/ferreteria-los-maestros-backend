package cl.losmaestros.ferreteria.service;

import cl.losmaestros.ferreteria.dto.CategoriaResponse;
import cl.losmaestros.ferreteria.repository.CategoriaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(CategoriaResponse::from)
                .toList();
    }
}
