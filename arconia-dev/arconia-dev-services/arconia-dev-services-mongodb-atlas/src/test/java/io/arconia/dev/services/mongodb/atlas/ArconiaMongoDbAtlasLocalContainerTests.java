package io.arconia.dev.services.mongodb.atlas;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaMongoDbAtlasLocalContainer}.
 */
class ArconiaMongoDbAtlasLocalContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaMongoDbAtlasLocalContainer(new MongoDbAtlasDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new MongoDbAtlasDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaMongoDbAtlasLocalContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaMongoDbAtlasLocalContainer.MONGODB_PORT));
    }

}
