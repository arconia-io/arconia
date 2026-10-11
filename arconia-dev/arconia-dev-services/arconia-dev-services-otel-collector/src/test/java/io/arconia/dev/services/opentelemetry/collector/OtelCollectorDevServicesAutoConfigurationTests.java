package io.arconia.dev.services.opentelemetry.collector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OtelCollectorDevServicesAutoConfiguration}.
 */
class OtelCollectorDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    OtelCollectorDevServicesAutoConfigurationTests() {
        super(OtelCollectorDevServicesAutoConfiguration.class, "otel-collector", ArconiaOtelCollectorContainer.class, OtlpTracingConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new OtelCollectorDevServicesProperties();
        return asDiscoverableContainer(new ArconiaOtelCollectorContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
        assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
        ArconiaOtelCollectorContainer container = (ArconiaOtelCollectorContainer) discoveredContainer;
        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF))
                .isEqualTo("http://%s:%d".formatted(container.getHost(), container.getHttpPort()) + OtlpTracingConnectionDetails.TRACES_PATH);
        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC)).isEqualTo("http://%s:%d".formatted(container.getHost(), container.getGrpcPort()));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArconiaOtelCollectorContainer.class));
    }

}
