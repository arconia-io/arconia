package io.arconia.opentelemetry.autoconfigure.sdk.exporter;

import io.opentelemetry.sdk.common.export.MemoryMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryExporterProperties}.
 */
class OpenTelemetryExporterPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenTelemetryExporterProperties.CONFIG_PREFIX).isEqualTo("arconia.otel.exporter");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();

        assertThat(properties.getOtlp()).isNotNull();
        assertThat(properties.getMemoryMode()).isEqualTo(MemoryMode.REUSABLE_DATA);
    }

    @Test
    void shouldUpdateMemoryMode() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();

        properties.setMemoryMode(MemoryMode.IMMUTABLE_DATA);

        assertThat(properties.getMemoryMode()).isEqualTo(MemoryMode.IMMUTABLE_DATA);
    }

    @Test
    void shouldProvideAccessToOtlpConfig() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();

        assertThat(properties.getOtlp())
            .isNotNull()
            .isInstanceOf(io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpExporterConfig.class);
    }

}
