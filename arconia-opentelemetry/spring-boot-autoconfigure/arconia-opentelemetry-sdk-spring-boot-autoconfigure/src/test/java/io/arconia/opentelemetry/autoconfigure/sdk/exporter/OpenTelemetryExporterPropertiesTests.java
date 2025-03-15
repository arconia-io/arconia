package io.arconia.opentelemetry.autoconfigure.sdk.exporter;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.sdk.common.export.MemoryMode;

import org.junit.jupiter.api.Test;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Protocol;

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
        assertThat(properties.getOtlp().getEndpoint()).isNull();
        assertThat(properties.getOtlp().getTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getOtlp().getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getOtlp().getProtocol()).isEqualTo(Protocol.HTTP_PROTOBUF);
        assertThat(properties.getOtlp().getCompression()).isEqualTo(Compression.GZIP);
        assertThat(properties.getOtlp().getHeaders()).isNotNull().isEmpty();
        assertThat(properties.getOtlp().isMetrics()).isFalse();
        assertThat(properties.getMemoryMode()).isEqualTo(MemoryMode.REUSABLE_DATA);
    }

    @Test
    void shouldUpdateEndpoint() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();
        URI endpoint = URI.create("http://localhost:4318/v1/traces");

        properties.getOtlp().setEndpoint(endpoint);

        assertThat(properties.getOtlp().getEndpoint()).isEqualTo(endpoint);
    }

    @Test
    void shouldUpdateTimeout() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();
        Duration timeout = Duration.ofSeconds(30);

        properties.getOtlp().setTimeout(timeout);

        assertThat(properties.getOtlp().getTimeout()).isEqualTo(timeout);
    }

    @Test
    void shouldUpdateConnectTimeout() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();
        Duration connectTimeout = Duration.ofSeconds(20);

        properties.getOtlp().setConnectTimeout(connectTimeout);

        assertThat(properties.getOtlp().getConnectTimeout()).isEqualTo(connectTimeout);
    }

    @Test
    void shouldUpdateProtocol() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();

        properties.getOtlp().setProtocol(Protocol.GRPC);

        assertThat(properties.getOtlp().getProtocol()).isEqualTo(Protocol.GRPC);
    }

    @Test
    void shouldUpdateCompression() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();

        properties.getOtlp().setCompression(Compression.NONE);

        assertThat(properties.getOtlp().getCompression()).isEqualTo(Compression.NONE);
    }

    @Test
    void shouldUpdateHeaders() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("Custom-Header", "value");

        properties.getOtlp().setHeaders(headers);

        assertThat(properties.getOtlp().getHeaders())
                .isNotNull()
                .hasSize(2)
                .containsEntry("Authorization", "Bearer token123")
                .containsEntry("Custom-Header", "value");
    }

    @Test
    void shouldUpdateMetrics() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();

        properties.getOtlp().setMetrics(true);

        assertThat(properties.getOtlp().isMetrics()).isTrue();
    }

    @Test
    void shouldUpdateMemoryMode() {
        OpenTelemetryExporterProperties properties = new OpenTelemetryExporterProperties();

        properties.setMemoryMode(MemoryMode.IMMUTABLE_DATA);

        assertThat(properties.getMemoryMode()).isEqualTo(MemoryMode.IMMUTABLE_DATA);
    }

}
