package be.imgn.mtg.engine.rules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * References one or more MTG Comprehensive Rules.
 *
 * <p>Use this annotation to document which game rules an element implements or relates to. The annotation is retained
 * at runtime, allowing tooling to verify rule coverage.
 *
 * <h2>Supported formats</h2>
 *
 * <ul>
 *   <li>Single rule: {@code "202.3a"}
 *   <li>Rule range: {@code "613.1-613.7"}
 *   <li>Section: {@code "704"}
 * </ul>
 *
 * <h2>Example usage</h2>
 *
 * <pre>{@code
 * @MtgRule("202.3a")
 * Value manaValue();
 *
 * @MtgRule("205.3g")
 * @MtgRule("205.3h")
 * public enum ArtifactType implements Subtype { ... }
 * }</pre>
 *
 * @see MtgRules
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Repeatable(MtgRules.class)
public @interface MtgRule {
    /**
     * The rule reference (e.g., "202.3a", "613.1-613.7", "704").
     *
     * @return the rule reference
     */
    String value();
}
