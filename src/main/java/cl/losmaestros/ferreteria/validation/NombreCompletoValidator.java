package cl.losmaestros.ferreteria.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Mismas reglas y mensajes que el campo "nombre" de userValidations.js. */
public class NombreCompletoValidator implements ConstraintValidator<NombreCompleto, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) return true;

        String nombre = valor.trim();
        String error = null;
        if (nombre.length() < 3 || nombre.length() > 60) error = "Debe tener entre 3 y 60 caracteres";
        else if (!nombre.matches("^\\p{L}[\\p{L} '-]*$")) error = "Solo letras, espacios, apóstrofes y guiones";
        else if (nombre.split("\\s+").length < 2) error = "Ingresa nombre y apellido";

        if (error == null) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }
}
