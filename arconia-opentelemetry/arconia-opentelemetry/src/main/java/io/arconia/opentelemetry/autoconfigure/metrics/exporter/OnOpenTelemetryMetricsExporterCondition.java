package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

import io.arconia.opentelemetry.autoconfigure.exporter.OnOpenTelemetryExporterCondition;

/**
 * Determines if a certain exporter type is enabled for OpenTelemetry metrics.
 */
class OnOpenTelemetryMetricsExporterCondition extends OnOpenTelemetryExporterCondition {

    OnOpenTelemetryMetricsExporterCondition() {
        super(ConditionalOnOpenTelemetryMetricsExporter.class, OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX);
    }

}
