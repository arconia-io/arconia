package io.arconia.opentelemetry.autoconfigure.logs.exporter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

import io.arconia.opentelemetry.autoconfigure.logs.ConditionalOnOpenTelemetryLogging;

/**
 * Whether OpenTelemetry logs should be exported using the specified exporter type.
 * <p>
 * An auto-configuration using this condition is ordered after {@code OpenTelemetryExporterAutoConfiguration},
 * so that the exporter properties contributed by auto-configurations ordered before it,
 * such as dev services, are taken into account.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnOpenTelemetryLogging
@Conditional(OnOpenTelemetryLoggingExporterCondition.class)
public @interface ConditionalOnOpenTelemetryLoggingExporter {

    /**
     * The type name of the OpenTelemetry logs exporter.
     */
    String value();

}
