package io.arconia.dev.services.floci;

import java.net.URI;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.floci.testcontainers.FlociContainer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlociDevServicesAutoConfiguration}.
 */
@DisabledIfDockerUnavailable
class FlociDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    FlociDevServicesAutoConfigurationTests() {
        super(FlociDevServicesAutoConfiguration.class, "floci", FlociContainer.class, AwsConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new FlociDevServicesProperties();
        return asDiscoverableContainer(FlociDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        FlociContainer flociContainer = (FlociContainer) discoveredContainer;
        AwsConnectionDetails connectionDetails = context.getBean(AwsConnectionDetails.class);
        assertThat(connectionDetails.getEndpoint()).isEqualTo(URI.create(flociContainer.getEndpoint()));
        assertThat(connectionDetails.getRegion()).isEqualTo(flociContainer.getRegion());
        assertThat(connectionDetails.getAccessKey()).isEqualTo(flociContainer.getAccessKey());
        assertThat(connectionDetails.getSecretKey()).isEqualTo(flociContainer.getSecretKey());
    }

}
