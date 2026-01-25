package io.arconia.dev.services.core.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Whether the specified dev services module should be enabled.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnDevServicesEnabledCondition.class)
public @interface ConditionalOnDevServicesEnabled {

    /**
     * The logical name of the dev services module to enable.
     */
    String value();

}
