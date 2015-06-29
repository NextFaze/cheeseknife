package com.nextfaze.cheeseknife;

import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Bind a method to an {@link View.OnLongClickListener OnLongClickListener} on the view for each ID specified.
 * <pre><code>
 * {@literal @}OnLongClick("example") void onClick() {
 *   Toast.makeText(this, "Clicked!", LENGTH_SHORT).show();
 * }
 * </code></pre>
 * Any number of parameters from
 * {@link View.OnLongClickListener#onLongClick(android.view.View) onClick} may be used on the
 * method.
 *
 * @see View.OnClickListener
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface OnLongClick {
    /**
     * View ids to which the method will be bound. (this has priority over String[] value()).
     * This cannot be used in library modules, as ids must be a constant value.
     */
    int[] id() default { 0 };

    /** View IDs to which the method will be bound. */
    String[] name() default { "" };
}
