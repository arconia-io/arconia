package io.arconia.dev.services.rabbitmq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.RabbitMQContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RabbitMqDevServicesAutoConfiguration}.
 */
class RabbitMqDevServicesAutoConfigurationTests {

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
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.rabbitmq.image-name=docker.io/rabbitmq",
                "arconia.dev.services.rabbitmq.environment.RABBITMQ_DEFAULT_USER=user",
                "arconia.dev.services.rabbitmq.shared=never"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RabbitMQContainer.class);
                RabbitMQContainer container = context.getBean(RabbitMQContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/rabbitmq");
                assertThat(container.getEnv()).contains("RABBITMQ_DEFAULT_USER=user");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
