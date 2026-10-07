package io.arconia.dev.services.floci;

import java.net.URI;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.floci.testcontainers.FlociContainer;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link FlociDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class FlociDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(FlociDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return FlociDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return FlociContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "floci";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return AwsConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new FlociDevServicesProperties();
        return asDiscoverableContainer(new ArconiaFlociContainer(properties), properties, ownerId);
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

    @Test
    void containerAvailableWithDefaultConfiguration() {
        getContextRunner()
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaFlociContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).noneMatch(e -> e.startsWith("KEY="));
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
