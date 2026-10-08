package io.arconia.dev.services.pulsar;

import org.springframework.boot.pulsar.autoconfigure.PulsarConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.pulsar.PulsarContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PulsarDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PulsarDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(PulsarDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return PulsarDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return PulsarContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "pulsar";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return PulsarConnectionDetails.class;
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
