package io.arconia.dev.services.pulsar;

import java.util.Map;

import org.testcontainers.pulsar.PulsarContainer;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaPulsarContainer}.
 */
class ArconiaPulsarContainerTests extends BaseDevServiceContainerTests<PulsarDevServicesProperties, ArconiaPulsarContainer> {

    @Override
    protected PulsarDevServicesProperties createProperties() {
        return new PulsarDevServicesProperties();
    }

    @Override
    protected ArconiaPulsarContainer createContainer(PulsarDevServicesProperties properties) {
        return new ArconiaPulsarContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(PulsarDevServicesProperties properties) {
        properties.setPort(1234);
        properties.setAdminPort(5678);
        return Map.of(
                1234, PulsarContainer.BROKER_PORT,
                5678, PulsarContainer.BROKER_HTTP_PORT);
    }

}
