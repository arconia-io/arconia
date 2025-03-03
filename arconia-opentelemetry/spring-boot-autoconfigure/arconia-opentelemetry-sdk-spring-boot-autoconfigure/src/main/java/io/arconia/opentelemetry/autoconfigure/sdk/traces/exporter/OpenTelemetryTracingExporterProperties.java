package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpExporterConfig;

/**
 * Configuration properties for OpenTelemetry tracing exporters.
 */
@ConfigurationProperties(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX)
public class OpenTelemetryTracingExporterProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.traces.exporter";

    /**
     * The type of OpenTelemetry exporter to use.
     */
    private ExporterType type = ExporterType.OTLP;

    /**
     * Options for the OTLP exporter.
     */
    @NestedConfigurationProperty
    private final OtlpExporterConfig otlp = new OtlpExporterConfig();

    public ExporterType getType() {
        return type;
    }

    public void setType(ExporterType type) {
        this.type = type;
    }

    public OtlpExporterConfig getOtlp() {
        return otlp;
    }

}
