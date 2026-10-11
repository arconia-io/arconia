package io.arconia.dev.services.elasticsearch;

import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticsearchDevServicesAutoConfiguration}.
 */
class ElasticsearchDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    ElasticsearchDevServicesAutoConfigurationTests() {
        super(ElasticsearchDevServicesAutoConfiguration.class, "elasticsearch", ElasticsearchContainer.class, ElasticsearchConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new ElasticsearchDevServicesProperties();
        return asDiscoverableContainer(new ArconiaElasticsearchContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        ElasticsearchConnectionDetails connectionDetails = context.getBean(ElasticsearchConnectionDetails.class);
        assertThat(connectionDetails.getNodes()).singleElement().satisfies(node -> {
            assertThat(node.hostname()).isEqualTo(discoveredContainer.getHost());
            assertThat(node.port()).isEqualTo(discoveredContainer.getMappedPort(ArconiaElasticsearchContainer.ELASTICSEARCH_DEFAULT_PORT));
        });
        assertThat(connectionDetails.getUsername()).isEqualTo("elastic");
        assertThat(connectionDetails.getPassword())
                .isEqualTo(discoveredContainer.getEnvMap().get("ELASTIC_PASSWORD"));
    }

}
