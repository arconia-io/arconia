package io.arconia.dev.services.garage;

import org.junit.jupiter.api.Test;

import io.arconia.testcontainers.garage.GarageContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaGarageContainer}.
 */
class ArconiaGarageContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaGarageContainer(new GarageDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    void whenExposedPortsAreConfigured() {
        var properties = new GarageDevServicesProperties();
        properties.setPort(13900);

        var container = new ArconiaGarageContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + GarageContainer.S3_API_PORT));
    }

    @Test
    void whenBucketNameOverridden() {
        var properties = new GarageDevServicesProperties();
        properties.setBucketName("custom-bucket");

        var container = new ArconiaGarageContainer(properties);

        assertThat(container.getDefaultBucket()).isEqualTo("custom-bucket");
    }

}
