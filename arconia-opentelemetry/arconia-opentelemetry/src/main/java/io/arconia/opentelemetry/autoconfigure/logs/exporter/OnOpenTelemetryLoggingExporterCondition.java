package io.arconia.opentelemetry.autoconfigure.logs.exporter;

import io.arconia.opentelemetry.autoconfigure.exporter.OnOpenTelemetryExporterCondition;

/**
 * Determines if a certain exporter type is enabled for OpenTelemetry logs.
 */
class OnOpenTelemetryLoggingExporterCondition extends OnOpenTelemetryExporterCondition {

    OnOpenTelemetryLoggingExporterCondition() {
        super(ConditionalOnOpenTelemetryLoggingExporter.class, OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX);
    }

}
