package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

import io.arconia.opentelemetry.autoconfigure.exporter.OnOpenTelemetryExporterCondition;

/**
 * Determines if a certain exporter type is enabled for OpenTelemetry metrics.
 */
class OnOpenTelemetryMetricsExporterCondition extends OnOpenTelemetryExporterCondition {

    OnOpenTelemetryMetricsExporterCondition() {
        // The exporter-specific 'management.otlp.metrics.export.enabled' belongs to the Spring Boot
        // OTLP configuration, which the Arconia one replaces.
        super(ConditionalOnOpenTelemetryMetricsExporter.class, OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX,
                "management.defaults.metrics.export.enabled");
    }

}
