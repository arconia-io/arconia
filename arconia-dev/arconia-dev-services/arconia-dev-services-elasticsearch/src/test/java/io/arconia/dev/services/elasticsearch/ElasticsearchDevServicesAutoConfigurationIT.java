package io.arconia.dev.services.elasticsearch;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
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
        return withDiscoveryLabels(new ArconiaElasticsearchContainer(new ElasticsearchDevServicesProperties()), ownerId);
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

    @Test
    void containerAvailableWithDefaultConfiguration() {
        getContextRunner()
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaElasticsearchContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).contains(
                            "discovery.type=single-node",
                            "cluster.routing.allocation.disk.threshold_enabled=false",
                            "ELASTIC_PASSWORD=" + ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);
                    assertThat(container.getNetworkAliases()).hasSize(1);

                    assertThatHasSingletonScope(context);
            });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties());

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    container.stop();
                });
    }

}
