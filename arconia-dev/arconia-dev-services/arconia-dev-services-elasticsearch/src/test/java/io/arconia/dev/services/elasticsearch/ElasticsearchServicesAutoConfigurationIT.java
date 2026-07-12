package io.arconia.dev.services.elasticsearch;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfDockerAvailable
public class ElasticsearchServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {
    private final ApplicationContextRunner contextRunner = defaultContextRunner(ElasticsearchServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return ElasticsearchServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return ElasticsearchContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "elasticsearch";
    }

    @Test
    void containerAvailableWithDefaultEnv() {
        getContextRunner().run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            var container = context.getBean(getContainerClass());
            assertThat(container.getDockerImageName()).contains(ArconiaElasticsearchContainer.COMPATIBLE_IMAGE_NAME);
            assertThat(container.getEnv()).contains(
                    "discovery.type=single-node",
                    "cluster.routing.allocation.disk.threshold_enabled=false",
                    "ELASTIC_PASSWORD="+ArconiaElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);
            assertThat(container.getNetworkAliases()).hasSize(1);
            container.start();
            assertThat(container.isRunning()).isTrue();
            container.stop();

            assertThatHasSingletonScope(context);
        });
    }

    @Test
    void containerInitializedAndBasicHealthCheckPass() {
        getContextRunner().run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            ArconiaElasticsearchContainer container = (ArconiaElasticsearchContainer) context.getBean(getContainerClass());
            container.start();
            assertThat(container.isRunning()).isTrue();
            var result = container.execInContainer(
                    "curl",
                    "-s",
                    "-k",
                    "-u",
                    "elastic:"+ArconiaElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD,
                    "-X",
                    "GET",
                    "https://localhost:9200/_cluster/health?pretty"
            );
            assertThat(result.getExitCode()).isEqualTo(0);
            assertThat(result.getStdout()).containsPattern(".*\"status\"\\s*:\\s*\"green\".*");
            container.stop();
            assertThat(container.isRunning()).isFalse();
        });
    }
}
