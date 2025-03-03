package io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtlpExporterConfig}.
 */
class OtlpExporterConfigTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OtlpExporterConfig config = new OtlpExporterConfig();

        assertThat(config.getEndpoint()).isNull();
        assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.getProtocol()).isEqualTo(Protocol.HTTP_PROTOBUF);
        assertThat(config.getCompression()).isEqualTo(Compression.GZIP);
        assertThat(config.getHeaders()).isNotNull().isEmpty();
        assertThat(config.isMetrics()).isFalse();
    }

    @Test
    void shouldUpdateEndpoint() {
        OtlpExporterConfig config = new OtlpExporterConfig();
        URI endpoint = URI.create("http://localhost:4318/v1/traces");

        config.setEndpoint(endpoint);

        assertThat(config.getEndpoint()).isEqualTo(endpoint);
    }

    @Test
    void shouldUpdateTimeout() {
        OtlpExporterConfig config = new OtlpExporterConfig();
        Duration timeout = Duration.ofSeconds(30);

        config.setTimeout(timeout);

        assertThat(config.getTimeout()).isEqualTo(timeout);
    }

    @Test
    void shouldUpdateConnectTimeout() {
        OtlpExporterConfig config = new OtlpExporterConfig();
        Duration connectTimeout = Duration.ofSeconds(20);

        config.setConnectTimeout(connectTimeout);

        assertThat(config.getConnectTimeout()).isEqualTo(connectTimeout);
    }

    @Test
    void shouldUpdateProtocol() {
        OtlpExporterConfig config = new OtlpExporterConfig();

        config.setProtocol(Protocol.GRPC);

        assertThat(config.getProtocol()).isEqualTo(Protocol.GRPC);
    }

    @Test
    void shouldUpdateCompression() {
        OtlpExporterConfig config = new OtlpExporterConfig();

        config.setCompression(Compression.NONE);

        assertThat(config.getCompression()).isEqualTo(Compression.NONE);
    }

    @Test
    void shouldUpdateHeaders() {
        OtlpExporterConfig config = new OtlpExporterConfig();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("Custom-Header", "value");

        config.setHeaders(headers);

        assertThat(config.getHeaders())
            .isNotNull()
            .hasSize(2)
            .containsEntry("Authorization", "Bearer token123")
            .containsEntry("Custom-Header", "value");
    }

    @Test
    void shouldUpdateMetrics() {
        OtlpExporterConfig config = new OtlpExporterConfig();

        config.setMetrics(true);

        assertThat(config.isMetrics()).isTrue();
    }

}
