package cl.losmaestros.ferreteria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Producto del catálogo. La clave es el código de negocio (2 letras + 3 números, ej. MC001).
 * Al eliminarlo solo se desactiva (activo = false), para no romper pedidos que ya lo incluyen.
 */
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @Column(length = 5)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 40)
    private String subcategoria;

    @Column(nullable = false, length = 30)
    private String marca;

    @Column(nullable = false, length = 30)
    private String unidad;

    @Column(name = "precio_compra", nullable = false)
    private int precioCompra;

    /** Precio de venta. */
    @Column(nullable = false)
    private int precio;

    @Column(nullable = false)
    private int stock;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    @Column(length = 120)
    private String descripcion;

    /** Ruta o URL de la imagen (ej. "/products/cemento.webp", servida por el frontend). */
    @Column(length = 255)
    private String imagen;

    @Column(nullable = false)
    private boolean activo = true;

    protected Producto() {
        // requerido por JPA
    }

    public Producto(String codigo) {
        this.codigo = codigo;
    }

    public EstadoStock getEstadoStock() {
        return EstadoStock.calcular(stock, stockMinimo);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getSubcategoria() {
        return subcategoria;
    }

    public void setSubcategoria(String subcategoria) {
        this.subcategoria = subcategoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public int getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(int precioCompra) {
        this.precioCompra = precioCompra;
    }

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
