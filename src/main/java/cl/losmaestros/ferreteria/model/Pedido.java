package cl.losmaestros.ferreteria.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Compra de un cliente. El total se calcula en el servidor a partir de sus líneas. */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.EN_PREPARACION;

    @Column(nullable = false)
    private long total;

    /** Las líneas se guardan y se borran junto con el pedido (cascade). */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetallePedido> detalles = new ArrayList<>();

    protected Pedido() {
        // requerido por JPA
    }

    public Pedido(Usuario cliente, LocalDateTime fecha) {
        this.cliente = cliente;
        this.fecha = fecha;
    }

    /** Agrega una línea al pedido y suma su subtotal al total. */
    public void agregarDetalle(Producto producto, int cantidad) {
        DetallePedido detalle = new DetallePedido(this, producto, cantidad);
        detalles.add(detalle);
        total += detalle.getSubtotal();
    }

    public Long getId() {
        return id;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public long getTotal() {
        return total;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }
}
