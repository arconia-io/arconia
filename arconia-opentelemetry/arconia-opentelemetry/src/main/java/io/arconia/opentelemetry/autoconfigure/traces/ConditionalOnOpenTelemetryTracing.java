package io.arconia.opentelemetry.autoconfigure.traces;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;

/**
 * Whether OpenTelemetry tracing support is eligible for registration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnOpenTelemetry
@Conditional(OnOpenTelemetryTracingCondition.class)
public @interface ConditionalOnOpenTelemetryTracing {

}
