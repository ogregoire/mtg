package be.imgn.mtg.engine.rules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link MtgRule} annotations.
 *
 * <p>This annotation is automatically used when multiple {@code @MtgRule} annotations are placed on the same element.
 *
 * @see MtgRule
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface MtgRules {
    /**
     * Returns the contained {@link MtgRule} annotations.
     *
     * @return array of MtgRule annotations
     */
    MtgRule[] value();
}
