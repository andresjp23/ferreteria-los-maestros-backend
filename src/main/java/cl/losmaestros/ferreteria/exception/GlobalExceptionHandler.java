package cl.losmaestros.ferreteria.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Convierte las excepciones en respuestas JSON con el formato de ErrorResponse. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation (@NotBlank, @Size...): un mensaje por campo, el primero que falle. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Revisa los datos ingresados", campos);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), campo(ex.getCampo(), ex.getMessage()));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), campo(ex.getCampo(), ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    /** JSON mal formado o un tipo incorrecto (ej. "abc" en un número). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + ex.getName(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex) {
        return respuesta(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido en esta ruta", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", Map.of());
    }

    private static Map<String, String> campo(String campo, String mensaje) {
        return campo == null ? Map.of() : Map.of(campo, mensaje);
    }

    private static ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String mensaje,
            Map<String, String> campos) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje, campos));
    }
}
