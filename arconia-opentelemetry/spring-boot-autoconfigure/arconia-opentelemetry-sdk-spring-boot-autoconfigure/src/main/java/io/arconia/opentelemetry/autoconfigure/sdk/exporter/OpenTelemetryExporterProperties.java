package io.arconia.opentelemetry.autoconfigure.sdk.exporter;

import io.opentelemetry.sdk.common.export.MemoryMode;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpExporterConfig;

/**
 * Configuration properties for OpenTelemetry exporters.
 */
@ConfigurationProperties(prefix = OpenTelemetryExporterProperties.CONFIG_PREFIX)
public class OpenTelemetryExporterProperties {

    public static final String CONFIG_PREFIX = "arconia.opentelemetry.exporter";

    /**
     * Common options for the OTLP exporters.
     */
    @NestedConfigurationProperty
    private final OtlpExporterConfig otlp = new OtlpExporterConfig();

    /**
     * Whether to reuse objects to reduce allocation or work with immutable data structures.
     */
    private MemoryMode memoryMode = MemoryMode.REUSABLE_DATA;

    public OtlpExporterConfig getOtlp() {
        return otlp;
    }

    public MemoryMode getMemoryMode() {
        return memoryMode;
    }

    public void setMemoryMode(MemoryMode memoryMode) {
        this.memoryMode = memoryMode;
    }

}
