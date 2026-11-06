package cl.losmaestros.ferreteria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Checkout: solo se envían los códigos y las cantidades del carrito ({ id, cantidad }, como lo guarda
 * el CartProvider). El precio y el total los calcula el servidor: no se confía en lo que diga el navegador.
 */
public record CrearPedidoRequest(
        @NotEmpty(message = "El carrito está vacío")
        List<@Valid Item> items) {

    public record Item(
            @NotBlank(message = "Falta el código del producto")
            String id,

            @NotNull(message = "Falta la cantidad")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            @Max(value = 1000, message = "La cantidad no puede superar 1.000")
            Integer cantidad) {
    }
}
