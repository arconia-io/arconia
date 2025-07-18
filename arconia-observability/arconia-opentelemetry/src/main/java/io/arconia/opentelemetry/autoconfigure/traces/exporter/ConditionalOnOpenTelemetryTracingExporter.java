package io.arconia.opentelemetry.autoconfigure.traces.exporter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

import io.arconia.opentelemetry.autoconfigure.traces.ConditionalOnOpenTelemetryTracing;

/**
 * Whether OpenTelemetry traces should be exported using the specified exporter type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnOpenTelemetryTracing
@Conditional(OnOpenTelemetryTracingExporterCondition.class)
public @interface ConditionalOnOpenTelemetryTracingExporter {

    /**
     * The type name of the OpenTelemetry traces exporter.
     */
    String value();

}
