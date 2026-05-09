package io.arconia.dev.services.openlit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaOpenLitContainer}.
 */
class ArconiaOpenLitContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaOpenLitContainer(new OpenLitDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    void whenExposedPortsAreConfigured() {
        var properties = new OpenLitDevServicesProperties();
        properties.setPort(1234);
        properties.setOtlpGrpcPort(5678);
        properties.setOtlpHttpPort(9067);

        var container = new ArconiaOpenLitContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(properties.getPort() + ":" + ArconiaOpenLitContainer.UI_PORT))
                .anyMatch(binding -> binding.startsWith(properties.getOtlpGrpcPort() + ":" + ArconiaOpenLitContainer.OTLP_GRPC_PORT))
                .anyMatch(binding -> binding.startsWith(properties.getOtlpHttpPort() + ":" + ArconiaOpenLitContainer.OTLP_HTTP_PORT));
    }

}
