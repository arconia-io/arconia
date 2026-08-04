package io.arconia.dev.services.kafka;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.kafka.KafkaContainer;

import io.arconia.dev.services.core.container.DevServiceLabels;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KafkaDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class KafkaDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(KafkaDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return KafkaDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return KafkaContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "kafka";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return KafkaConnectionDetails.class;
    }

    @Override
    protected boolean supportsSharing() {
        return true;
    }

    @Override
    protected GenericContainer<?> createSharedContainer(String ownerId) {
        KafkaDevServicesProperties properties = new KafkaDevServicesProperties();
        return withSharedLabels(new KafkaContainer(properties.getImageName()), ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> sharedContainer) {
        KafkaConnectionDetails connectionDetails = context.getBean(KafkaConnectionDetails.class);
        assertThat(connectionDetails.getBootstrapServers()).hasSize(1);
        assertThat(connectionDetails.getBootstrapServers().getFirst())
                .endsWith(":" + sharedContainer.getMappedPort(ArconiaKafkaContainer.KAFKA_PORT));
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaContainer.class);
                    KafkaContainer container = context.getBean(KafkaContainer.class);
                    assertThat(container.getDockerImageName()).contains("apache/kafka-native");
                    assertThat(container.getEnv()).isNotEmpty(); // Configured by Testcontainers.
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getBinds()).isEmpty();
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "kafka")
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
