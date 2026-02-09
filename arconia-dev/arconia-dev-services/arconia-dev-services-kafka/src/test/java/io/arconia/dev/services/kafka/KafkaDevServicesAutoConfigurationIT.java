package io.arconia.dev.services.kafka;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.kafka.KafkaContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KafkaDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class KafkaDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private static final ApplicationContextRunner contextRunner = defaultContextRunner(KafkaDevServicesAutoConfiguration.class);

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
                    assertThat(container.isShouldBeReused()).isTrue();
                    assertThat(container.getBinds()).isEmpty();

                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(KafkaContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("singleton");
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
