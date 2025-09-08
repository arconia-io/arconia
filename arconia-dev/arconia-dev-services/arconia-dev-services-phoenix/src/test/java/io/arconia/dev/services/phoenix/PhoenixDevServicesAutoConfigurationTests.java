package io.arconia.dev.services.phoenix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PhoenixDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PhoenixDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(PhoenixDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.phoenix.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(GenericContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(GenericContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(GenericContainer.class);
                    GenericContainer<?> container = context.getBean(GenericContainer.class);
                    assertThat(container.getDockerImageName()).contains("arizephoenix/phoenix");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isTrue();
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(GenericContainer.class);
                    GenericContainer<?> container = context.getBean(GenericContainer.class);
                    assertThat(container.getDockerImageName()).contains("arizephoenix/phoenix");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.phoenix.image-name=docker.io/arizephoenix/phoenix",
                "arconia.dev.services.phoenix.environment.PHOENIX_WORKING_DIR=/fawkes",
                "arconia.dev.services.phoenix.shared=never",
                "arconia.dev.services.phoenix.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(GenericContainer.class);
                GenericContainer<?> container = context.getBean(GenericContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/arizephoenix/phoenix");
                assertThat(container.getEnv()).contains("PHOENIX_WORKING_DIR=/fawkes");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
