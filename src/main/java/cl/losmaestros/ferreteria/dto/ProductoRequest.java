package cl.losmaestros.ferreteria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar un producto. Los nombres de los campos son los mismos del formulario
 * del frontend (AddProductForm), así cada error se puede mostrar en su campo.
 * Las reglas y los mensajes copian los de frontend/src/utils/productValidation.js.
 */
public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^\\s*[A-Za-z]{2}\\d{3}\\s*$",
                message = "Formato inválido: 2 letras mayúsculas y 3 números (ej. MC001)")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 80, message = "Debe tener entre 3 y 80 caracteres")
        @Pattern(regexp = ".*\\p{L}.*", message = "Debe incluir al menos una letra")
        String nombre,

        @NotBlank(message = "Selecciona una categoría")
        String categoria,

        @NotBlank(message = "La subcategoría es obligatoria")
        @Size(min = 2, max = 40, message = "Debe tener entre 2 y 40 caracteres")
        @Pattern(regexp = ".*\\p{L}.*", message = "Debe incluir al menos una letra")
        String subcategoria,

        @NotBlank(message = "La marca es obligatoria")
        @Size(min = 2, max = 30, message = "Debe tener entre 2 y 30 caracteres")
        @Pattern(regexp = "^[\\p{L}\\d .+&'-]+$", message = "Solo letras, números y los símbolos . + & ' -")
        String marca,

        @NotBlank(message = "Selecciona una unidad")
        @Size(max = 30, message = "Máximo 30 caracteres")
        String unidad,

        @NotNull(message = "El precio de compra es obligatorio")
        @Min(value = 1, message = "Debe ser mayor a 0")
        @Max(value = 9_999_999, message = "No puede superar 9.999.999")
        Integer precioCompra,

        @NotNull(message = "El precio de venta es obligatorio")
        @Min(value = 1, message = "Debe ser mayor a 0")
        @Max(value = 9_999_999, message = "No puede superar 9.999.999")
        Integer precioVenta,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "No puede ser negativo")
        @Max(value = 100_000, message = "No puede superar 100.000")
        Integer stock,

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "No puede ser negativo")
        @Max(value = 100_000, message = "No puede superar 100.000")
        Integer stockMinimo,

        @Size(max = 120, message = "Máximo 120 caracteres")
        String descripcion,

        @Size(max = 255, message = "Máximo 255 caracteres")
        String imagen) {
}
