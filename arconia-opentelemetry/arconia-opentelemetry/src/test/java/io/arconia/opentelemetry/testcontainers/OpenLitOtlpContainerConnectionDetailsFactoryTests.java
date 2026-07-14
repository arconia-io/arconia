package io.arconia.opentelemetry.testcontainers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingExporterConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenLitOtlpContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@TestPropertySource(properties = {
        "arconia.otel.logs.exporter.type=none",
        "arconia.otel.metrics.exporter.type=none",
        "arconia.otel.traces.exporter.type=none"
})
class OpenLitOtlpContainerConnectionDetailsFactoryTests extends OpenLitTestcontainers {

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
        assertThat(url).isEqualTo("http://%s:%d%s".formatted(openLitContainer.getHost(),
                openLitContainer.getOtlpHttpPort(), OtlpTracingConnectionDetails.TRACES_PATH));
    }

    @Test
    void shouldProvideTracesConnectionDetailsForGrpc() {
        String url = tracingConnectionDetails.getTracesUrl(Protocol.GRPC);
        assertThat(url).isEqualTo("http://%s:%d".formatted(openLitContainer.getHost(),
                openLitContainer.getOtlpGrpcPort()));
    }

    @Test
    void shouldProvideMetricsConnectionDetailsForHttpProtobuf() {
        String url = metricsConnectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo("http://%s:%d%s".formatted(openLitContainer.getHost(),
                openLitContainer.getOtlpHttpPort(), OtlpMetricsConnectionDetails.METRICS_PATH));
    }

    @Test
    void shouldProvideMetricsConnectionDetailsForGrpc() {
        String url = metricsConnectionDetails.getMetricsUrl(Protocol.GRPC);
        assertThat(url).isEqualTo("http://%s:%d".formatted(openLitContainer.getHost(),
                openLitContainer.getOtlpGrpcPort()));
    }

    @Test
    void shouldProvideLogsConnectionDetailsForHttpProtobuf() {
        String url = loggingConnectionDetails.getLogsUrl(Protocol.HTTP_PROTOBUF);
        assertThat(url).isEqualTo("http://%s:%d%s".formatted(openLitContainer.getHost(),
                openLitContainer.getOtlpHttpPort(), OtlpLoggingConnectionDetails.LOGS_PATH));
    }

    @Test
    void shouldProvideLogsConnectionDetailsForGrpc() {
        String url = loggingConnectionDetails.getLogsUrl(Protocol.GRPC);
        assertThat(url).isEqualTo("http://%s:%d".formatted(openLitContainer.getHost(),
                openLitContainer.getOtlpGrpcPort()));
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({ OtlpLoggingExporterConfiguration.class, OtlpMetricsExporterConfiguration.class,
            OtlpTracingExporterConfiguration.class })
    static class TestConfiguration {}

}
