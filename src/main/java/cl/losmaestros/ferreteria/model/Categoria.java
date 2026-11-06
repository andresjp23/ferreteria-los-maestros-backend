package cl.losmaestros.ferreteria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Categoría del catálogo. El id es el slug que usa el frontend en la URL (ej. "herramientas"). */
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 60)
    private String nombre;

    protected Categoria() {
        // requerido por JPA
    }

    public Categoria(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
