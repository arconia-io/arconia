package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtlpExporterConfig}.
 */
class OtlpExporterConfigTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OtlpExporterConfig config = new OtlpExporterConfig();

        assertThat(config.getEndpoint()).isNull();
        assertThat(config.getTimeout()).isNull();
        assertThat(config.getConnectTimeout()).isNull();
        assertThat(config.getProtocol()).isNull();
        assertThat(config.getCompression()).isNull();
        assertThat(config.getRetry()).isNull();
        assertThat(config.getHeaders()).isEmpty();
        assertThat(config.isMetrics()).isNull();
    }

    @Test
    void shouldUpdateValues() {
        OtlpExporterConfig config = new OtlpExporterConfig();

        URI endpoint = URI.create("http://localhost:4318/v1/traces");
        config.setEndpoint(endpoint);

        Duration timeout = Duration.ofSeconds(30);
        config.setTimeout(timeout);

        Duration connectTimeout = Duration.ofSeconds(20);
        config.setConnectTimeout(connectTimeout);

        config.setProtocol(Protocol.GRPC);
        config.setCompression(Compression.NONE);

        config.setRetry(new RetryConfig());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("Custom-Header", "value");
        config.setHeaders(headers);

        config.setMetrics(true);

        assertThat(config.getEndpoint()).isEqualTo(endpoint);
        assertThat(config.getTimeout()).isEqualTo(timeout);
        assertThat(config.getConnectTimeout()).isEqualTo(connectTimeout);
        assertThat(config.getProtocol()).isEqualTo(Protocol.GRPC);
        assertThat(config.getCompression()).isEqualTo(Compression.NONE);
        assertThat(config.getRetry()).isNotNull();
        assertThat(config.getHeaders())
                .isNotNull()
                .hasSize(2)
                .containsEntry("Authorization", "Bearer token123")
                .containsEntry("Custom-Header", "value");
        assertThat(config.isMetrics()).isTrue();
    }

}
