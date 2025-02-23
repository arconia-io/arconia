package io.arconia.opentelemetry.autoconfigure.sdk;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Indicates when OpenTelemetry tracing support is eligible for registration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnEnabledOpenTelemetryCondition.class)
public @interface ConditionalOnOpenTelemetry {

    boolean enabled() default true;

}
