package io.arconia.dev.services.elasticsearch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaElasticsearchContainer}.
 */
class ArconiaElasticsearchContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaElasticsearchContainer(new ElasticsearchDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new ElasticsearchDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaElasticsearchContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaElasticsearchContainer.ELASTICSEARCH_DEFAULT_PORT));
    }

}
