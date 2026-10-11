package io.arconia.dev.services.pulsar;

import org.springframework.boot.pulsar.autoconfigure.PulsarConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.pulsar.PulsarContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PulsarDevServicesAutoConfiguration}.
 */
class PulsarDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    PulsarDevServicesAutoConfigurationTests() {
        super(PulsarDevServicesAutoConfiguration.class, "pulsar", PulsarContainer.class, PulsarConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new PulsarDevServicesProperties();
        return asDiscoverableContainer(new ArconiaPulsarContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        PulsarContainer container = (PulsarContainer) discoveredContainer;
        PulsarConnectionDetails connectionDetails = context.getBean(PulsarConnectionDetails.class);
        assertThat(connectionDetails.getBrokerUrl()).isEqualTo(container.getPulsarBrokerUrl());
        assertThat(connectionDetails.getAdminUrl()).isEqualTo(container.getHttpServiceUrl());
    }

}
