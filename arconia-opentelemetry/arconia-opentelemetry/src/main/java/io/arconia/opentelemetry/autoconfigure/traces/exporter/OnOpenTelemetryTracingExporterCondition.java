package io.arconia.opentelemetry.autoconfigure.traces.exporter;

import io.arconia.opentelemetry.autoconfigure.exporter.OnOpenTelemetryExporterCondition;

/**
 * Determines if a certain exporter type is enabled for OpenTelemetry traces.
 */
class OnOpenTelemetryTracingExporterCondition extends OnOpenTelemetryExporterCondition {

    OnOpenTelemetryTracingExporterCondition() {
        super(ConditionalOnOpenTelemetryTracingExporter.class, OpenTelemetryTracingExporterProperties.CONFIG_PREFIX);
    }

}
