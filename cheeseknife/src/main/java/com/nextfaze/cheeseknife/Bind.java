package com.nextfaze.cheeseknife;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Bind a field to the view for the specified ID. The view will automatically be cast to the field
 * type.
 * <pre><code>
 * {@literal @}Bind(R.id.title) TextView title;
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
public @interface Bind {
    /**
     * View id to which the field will be bound. (this has priority over String value()).
     * This cannot be used in library modules, as id must be a constant value.
     */
    int id() default 0;

    /**
     * View name to which the field will be bound.
     */
    String name() default "";
}
