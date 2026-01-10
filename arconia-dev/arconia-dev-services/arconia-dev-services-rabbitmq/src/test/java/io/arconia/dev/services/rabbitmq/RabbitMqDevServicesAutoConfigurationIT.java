package io.arconia.dev.services.rabbitmq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RabbitMqDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class RabbitMqDevServicesAutoConfigurationIT {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(RabbitMqDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.rabbitmq.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(RabbitMQContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(RabbitMQContainer.class);
                    RabbitMQContainer container = context.getBean(RabbitMQContainer.class);
                    assertThat(container.getDockerImageName()).contains("rabbitmq");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isTrue();
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(RabbitMQContainer.class);
                    RabbitMQContainer container = context.getBean(RabbitMQContainer.class);
                    assertThat(container.getDockerImageName()).contains("rabbitmq");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                    "arconia.dev.services.rabbitmq.environment.KEY=value",
                    "arconia.dev.services.rabbitmq.shared=never",
                    "arconia.dev.services.rabbitmq.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RabbitMQContainer.class);
                RabbitMQContainer container = context.getBean(RabbitMQContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(RabbitMQContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(RabbitMQContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
