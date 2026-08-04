package io.arconia.dev.services.pulsar;

import java.time.Duration;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.pulsar.autoconfigure.PulsarConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.DevServiceLabels;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PulsarDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PulsarDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(PulsarDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return PulsarDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return PulsarContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "pulsar";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return PulsarConnectionDetails.class;
    }

    @Override
    protected boolean supportsSharing() {
        return true;
    }

    @Override
    protected GenericContainer<?> createSharedContainer(String ownerId) {
        PulsarDevServicesProperties properties = new PulsarDevServicesProperties();
        PulsarContainer container = new PulsarContainer(DockerImageName.parse(properties.getImageName()))
                .withStartupTimeout(Duration.ofMinutes(2));
        return withSharedLabels(container, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> sharedContainer) {
        PulsarConnectionDetails connectionDetails = context.getBean(PulsarConnectionDetails.class);
        assertThat(connectionDetails.getBrokerUrl())
                .startsWith("pulsar://")
                .endsWith(":" + sharedContainer.getMappedPort(PulsarContainer.BROKER_PORT));
        assertThat(connectionDetails.getAdminUrl())
                .startsWith("http://")
                .endsWith(":" + sharedContainer.getMappedPort(PulsarContainer.BROKER_HTTP_PORT));
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaPulsarContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "pulsar")
                            .containsEntry(DevServiceLabels.SHARED, "true")
                            .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());

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
