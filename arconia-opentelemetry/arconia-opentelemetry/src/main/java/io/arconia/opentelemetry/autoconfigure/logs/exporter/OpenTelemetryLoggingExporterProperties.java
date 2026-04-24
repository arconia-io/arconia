package io.arconia.opentelemetry.autoconfigure.logs.exporter;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpExporterConfig;

/**
 * Configuration properties for exporting OpenTelemetry logs.
 */
@ConfigurationProperties(prefix = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX)
public class OpenTelemetryLoggingExporterProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.logs.exporter";

    /**
     * The type of OpenTelemetry exporter to use for logs.
     */
    @Nullable
    private ExporterType type;

    /**
     * Options for the OTLP log exporter.
     */
    @NestedConfigurationProperty
    private final OtlpExporterConfig otlp = new OtlpExporterConfig();

    @Nullable
    public ExporterType getType() {
        return type;
    }

    public void setType(@Nullable ExporterType type) {
        this.type = type;
    }

    public OtlpExporterConfig getOtlp() {
        return otlp;
    }

}
