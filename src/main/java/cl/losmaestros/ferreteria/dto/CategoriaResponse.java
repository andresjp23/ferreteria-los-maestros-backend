package cl.losmaestros.ferreteria.dto;

import cl.losmaestros.ferreteria.model.Categoria;

public record CategoriaResponse(String id, String nombre) {

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
