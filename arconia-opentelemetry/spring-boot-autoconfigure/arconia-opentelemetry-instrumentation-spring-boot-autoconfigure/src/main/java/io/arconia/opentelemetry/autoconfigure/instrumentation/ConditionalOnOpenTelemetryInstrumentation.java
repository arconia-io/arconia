package io.arconia.opentelemetry.autoconfigure.instrumentation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

import io.arconia.opentelemetry.autoconfigure.sdk.ConditionalOnOpenTelemetry;

/**
 * Indicates when OpenTelemetry Instrumentation is eligible for registration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnOpenTelemetry
@Conditional(OnOpenTelemetryInstrumentationCondition.class)
public @interface ConditionalOnOpenTelemetryInstrumentation {

    /**
     * The instrumentation name.
     */
    String value() default "";

}
