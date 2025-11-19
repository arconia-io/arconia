package io.arconia.opentelemetry.testcontainers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingExporterConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtelCollectorOtlpTracingContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@TestPropertySource(properties = "arconia.otel.traces.exporter.type=none")
class OtelCollectorOtlpTracingContainerConnectionDetailsFactoryTests extends OtelCollectorTestcontainers {

    @Autowired
    OtlpTracingConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        String url = connectionDetails.getUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpTracingConnectionDetails.DEFAULT_HTTP_PORT)
                + OtlpTracingConnectionDetails.TRACES_PATH);
    }

    @Test
    void shouldProvideConnectionDetailsForGrpc() {
        String url = connectionDetails.getUrl(Protocol.GRPC);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpTracingConnectionDetails.DEFAULT_GRPC_PORT));
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OtlpTracingExporterConfiguration.class)
    static class TestConfiguration {}

}
