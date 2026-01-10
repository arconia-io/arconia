package io.arconia.dev.services.opentelemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenTelemetryDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OpenTelemetryDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(OpenTelemetryDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.otel-collector.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArconiaOpenTelemetryCollectorContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArconiaOpenTelemetryCollectorContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(ArconiaOpenTelemetryCollectorContainer.class);
                    ArconiaOpenTelemetryCollectorContainer container = context.getBean(ArconiaOpenTelemetryCollectorContainer.class);
                    assertThat(container.getDockerImageName()).contains("otel/opentelemetry-collector-contrib");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isTrue();
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(ArconiaOpenTelemetryCollectorContainer.class);
                    ArconiaOpenTelemetryCollectorContainer container = context.getBean(ArconiaOpenTelemetryCollectorContainer.class);
                    assertThat(container.getDockerImageName()).contains("otel/opentelemetry-collector-contrib");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.otel-collector.environment.KEY=value",
                        "arconia.dev.services.otel-collector.shared=never",
                        "arconia.dev.services.otel-collector.startup-timeout=90s"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ArconiaOpenTelemetryCollectorContainer.class);
                    ArconiaOpenTelemetryCollectorContainer container = context.getBean(ArconiaOpenTelemetryCollectorContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(ArconiaOpenTelemetryCollectorContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(ArconiaOpenTelemetryCollectorContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
