package io.arconia.opentelemetry.autoconfigure.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

import io.arconia.opentelemetry.autoconfigure.ConditionalOnEnabledOpenTelemetry;

/**
 * Whether OpenTelemetry metrics support is eligible for registration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnEnabledOpenTelemetry
@Conditional(OnEnabledOpenTelemetryMetricsCondition.class)
public @interface ConditionalOnEnabledOpenTelemetryMetrics {

}
