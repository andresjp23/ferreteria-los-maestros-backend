package cl.losmaestros.ferreteria.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contraseña de 8 a 64 caracteres, sin espacios, con minúscula, mayúscula y número.
 * Un valor vacío se considera válido: la obligatoriedad se marca aparte con @NotBlank.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContrasenaSeguraValidator.class)
public @interface ContrasenaSegura {

    String message() default "Contraseña no válida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
