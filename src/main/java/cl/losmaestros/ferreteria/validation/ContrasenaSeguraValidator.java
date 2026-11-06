package cl.losmaestros.ferreteria.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Revisa las reglas en el mismo orden y con los mismos mensajes que userValidations.js. */
public class ContrasenaSeguraValidator implements ConstraintValidator<ContrasenaSegura, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isEmpty()) return true;

        String error = null;
        if (valor.length() < 8 || valor.length() > 64) error = "Debe tener entre 8 y 64 caracteres";
        else if (valor.chars().anyMatch(Character::isWhitespace)) error = "No puede contener espacios";
        else if (!valor.matches(".*[a-z].*")) error = "Debe incluir al menos una letra minúscula";
        else if (!valor.matches(".*[A-Z].*")) error = "Debe incluir al menos una letra mayúscula";
        else if (!valor.matches(".*\\d.*")) error = "Debe incluir al menos un número";

        if (error == null) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }
}
