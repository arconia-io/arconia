package io.arconia.dev.services.elasticsearch;

import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ElasticsearchDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class ElasticsearchDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(ElasticsearchDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return ElasticsearchDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return ElasticsearchContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "elasticsearch";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return ElasticsearchConnectionDetails.class;
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
