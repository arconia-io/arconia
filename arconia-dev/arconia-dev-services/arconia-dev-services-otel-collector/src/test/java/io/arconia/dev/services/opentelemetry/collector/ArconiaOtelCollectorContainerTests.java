package io.arconia.dev.services.opentelemetry.collector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaOtelCollectorContainer}.
 */
class ArconiaOtelCollectorContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaOtelCollectorContainer(new OtelCollectorDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new OtelCollectorDevServicesProperties();
        properties.setPort(1234);
        properties.setOtlpGrpcPort(5678);

        var container = new ArconiaOtelCollectorContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaOtelCollectorContainer.OTLP_HTTP_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getOtlpGrpcPort() + ":" + ArconiaOtelCollectorContainer.OTLP_GRPC_PORT));
    }

}
