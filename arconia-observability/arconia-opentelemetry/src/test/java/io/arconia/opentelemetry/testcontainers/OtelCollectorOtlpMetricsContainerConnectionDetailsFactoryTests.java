package io.arconia.opentelemetry.testcontainers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtelCollectorOtlpMetricsContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@TestPropertySource(properties = "arconia.otel.metrics.exporter.type=none")
class OtelCollectorOtlpMetricsContainerConnectionDetailsFactoryTests extends OtelCollectorTestcontainers {

    @Autowired
    OtlpMetricsConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        String url = connectionDetails.getUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpMetricsConnectionDetails.DEFAULT_HTTP_PORT)
                + OtlpMetricsConnectionDetails.METRICS_PATH);
    }

    @Test
    void shouldProvideConnectionDetailsForGrpc() {
        String url = connectionDetails.getUrl(Protocol.GRPC);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpMetricsConnectionDetails.DEFAULT_GRPC_PORT));
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OtlpMetricsExporterConfiguration.class)
    static class TestConfiguration {}

}
