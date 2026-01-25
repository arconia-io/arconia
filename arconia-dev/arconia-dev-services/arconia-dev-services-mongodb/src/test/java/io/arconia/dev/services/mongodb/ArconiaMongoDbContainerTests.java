package io.arconia.dev.services.mongodb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaMongoDbContainer}.
 */
class ArconiaMongoDbContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaMongoDbContainer(new MongoDbDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new MongoDbDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaMongoDbContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaMongoDbContainer.MONGODB_PORT));
    }

}
