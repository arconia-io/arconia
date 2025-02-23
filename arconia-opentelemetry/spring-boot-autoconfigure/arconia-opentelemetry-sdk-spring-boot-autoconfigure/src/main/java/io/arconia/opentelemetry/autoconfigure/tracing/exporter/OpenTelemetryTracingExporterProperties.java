package io.arconia.opentelemetry.autoconfigure.tracing.exporter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpExporterConfig;

/**
 * Configuration properties for OpenTelemetry tracing exporters.
 */
@ConfigurationProperties(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX)
public class OpenTelemetryTracingExporterProperties {

    public static final String CONFIG_PREFIX = "arconia.opentelemetry.traces.exporter";

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
