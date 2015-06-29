package com.nextfaze.cheeseknife;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Denote that the view specified by the binding is not required to be present.
 * <pre><code>
 * {@literal @}Optional @Bind(id = R.id.title) TextView subtitleView;
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD })
public @interface Optional {
}

