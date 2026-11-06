package cl.losmaestros.ferreteria.exception;

import java.util.Map;

/**
 * Formato único de error de la API.
 * campos: error por campo del formulario ({ "codigo": "Ya existe..." }); vacío si el error es general.
 */
public record ErrorResponse(int status, String error, String mensaje, Map<String, String> campos) {
}
