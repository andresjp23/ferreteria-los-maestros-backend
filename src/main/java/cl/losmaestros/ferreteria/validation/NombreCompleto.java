package cl.losmaestros.ferreteria.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Nombre y apellido: 3 a 60 caracteres, solo letras, espacios, apóstrofes y guiones. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NombreCompletoValidator.class)
public @interface NombreCompleto {

    String message() default "Nombre no válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
