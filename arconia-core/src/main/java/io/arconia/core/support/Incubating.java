package io.arconia.core.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a feature is incubating and may change in future releases
 * without notice, a deprecation period, or guarantees of backward compatibility.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
public @interface Incubating {

    /**
     * The version at which the feature was introduced.
     */
    String since() default "";

}
