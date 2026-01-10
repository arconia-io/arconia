package io.arconia.dev.services.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.kafka.KafkaContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KafkaDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class KafkaDevServicesAutoConfigurationIT {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(KafkaDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.kafka.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(KafkaContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaContainer.class);
                    KafkaContainer container = context.getBean(KafkaContainer.class);
                    assertThat(container.getDockerImageName()).contains("apache/kafka-native");
                    assertThat(container.isShouldBeReused()).isTrue();
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaContainer.class);
                    KafkaContainer container = context.getBean(KafkaContainer.class);
                    assertThat(container.getDockerImageName()).contains("apache/kafka-native");
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.kafka.environment.KEY=value",
                "arconia.dev.services.kafka.shared=never",
                "arconia.dev.services.kafka.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(KafkaContainer.class);
                KafkaContainer container = context.getBean(KafkaContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(KafkaContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
