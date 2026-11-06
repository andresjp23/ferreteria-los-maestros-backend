package cl.losmaestros.ferreteria.dto;

import jakarta.validation.constraints.NotBlank;

/** Nuevo estado del pedido: "Enviado" o "Entregado". */
public record CambioEstadoRequest(@NotBlank(message = "Selecciona un estado") String estado) {
}
