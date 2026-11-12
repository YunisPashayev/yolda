package az.turboclone.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public interface ValidImage {
    String message() default "Yalnız JPEG, PNG və ya WEBP formatında olan şəkillər qəbul edilir";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
}
