package io.arconia.dev.services.phoenix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PhoenixDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PhoenixDevServicesAutoConfigurationIT {

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
            .run(context -> assertThat(context).doesNotHaveBean(PhoenixContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(PhoenixContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(PhoenixContainer.class);
                    PhoenixContainer container = context.getBean(PhoenixContainer.class);
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
                    assertThat(context).hasSingleBean(PhoenixContainer.class);
                    PhoenixContainer container = context.getBean(PhoenixContainer.class);
                    assertThat(container.getDockerImageName()).contains("arizephoenix/phoenix");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.phoenix.environment.KEY=value",
                "arconia.dev.services.phoenix.shared=never",
                "arconia.dev.services.phoenix.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PhoenixContainer.class);
                PhoenixContainer container = context.getBean(PhoenixContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(PhoenixContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(PhoenixContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
