package io.arconia.opentelemetry.testcontainers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingExporterConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtelCollectorOtlpContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@TestPropertySource(properties = {
        "arconia.otel.logs.exporter.type=none",
        "arconia.otel.metrics.exporter.type=none",
        "arconia.otel.traces.exporter.type=none"
})
class OtelCollectorOtlpContainerConnectionDetailsFactoryTests extends OtelCollectorTestcontainers {

    @Autowired
    OtlpTracingConnectionDetails tracingConnectionDetails;

    @Autowired
    OtlpMetricsConnectionDetails metricsConnectionDetails;

    @Autowired
    OtlpLoggingConnectionDetails loggingConnectionDetails;

    @Test
    void shouldProvideSingleConnectionDetailsForAllSignals() {
        assertThat(tracingConnectionDetails).isSameAs(metricsConnectionDetails).isSameAs(loggingConnectionDetails);
    }

    @Test
    void shouldProvideTracesConnectionDetailsForHttpProtobuf() {
        String url = tracingConnectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpConnectionDetails.DEFAULT_HTTP_PORT)
                + OtlpTracingConnectionDetails.TRACES_PATH);
    }

    @Test
    void shouldProvideTracesConnectionDetailsForGrpc() {
        String url = tracingConnectionDetails.getTracesUrl(Protocol.GRPC);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpConnectionDetails.DEFAULT_GRPC_PORT));
    }

    @Test
    void shouldProvideMetricsConnectionDetailsForHttpProtobuf() {
        String url = metricsConnectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpConnectionDetails.DEFAULT_HTTP_PORT)
                + OtlpMetricsConnectionDetails.METRICS_PATH);
    }

    @Test
    void shouldProvideMetricsConnectionDetailsForGrpc() {
        String url = metricsConnectionDetails.getMetricsUrl(Protocol.GRPC);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpConnectionDetails.DEFAULT_GRPC_PORT));
    }

    @Test
    void shouldProvideLogsConnectionDetailsForHttpProtobuf() {
        String url = loggingConnectionDetails.getLogsUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpConnectionDetails.DEFAULT_HTTP_PORT)
                + OtlpLoggingConnectionDetails.LOGS_PATH);
    }

    @Test
    void shouldProvideLogsConnectionDetailsForGrpc() {
        String url = loggingConnectionDetails.getLogsUrl(Protocol.GRPC);
        assertThat(url).isEqualTo("http://" + otelCollectorContainer.getHost() + ":"
                + otelCollectorContainer.getMappedPort(OtlpConnectionDetails.DEFAULT_GRPC_PORT));
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({ OtlpLoggingExporterConfiguration.class, OtlpMetricsExporterConfiguration.class,
            OtlpTracingExporterConfiguration.class })
    static class TestConfiguration {}

}
