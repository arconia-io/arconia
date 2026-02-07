package io.arconia.dev.services.phoenix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
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
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PhoenixContainer.class));
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
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isTrue();

                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(PhoenixContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("singleton");
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(PhoenixContainer.class);
                    PhoenixContainer container = context.getBean(PhoenixContainer.class);
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues(
                        "arconia.dev.services.phoenix.environment.KEY=value",
                        "arconia.dev.services.phoenix.network-aliases=network1",
                        "arconia.dev.services.phoenix.resources[0].source-path=test-resource.txt",
                        "arconia.dev.services.phoenix.resources[0].container-path=/tmp/test-resource.txt"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PhoenixContainer.class);
                    PhoenixContainer container = context.getBean(PhoenixContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.getNetworkAliases()).contains("network1");
                    container.start();
                    assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");
                    //assertThat(container.execInContainer("ls", "/tmp").getStdout()).contains("test-resource.txt");
                    container.stop();
                });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .withInitializer(context -> {
                    context.getBeanFactory().registerScope("restart", new SimpleThreadScope());
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(PhoenixContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(PhoenixContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

    @Test
    void customDefaultPropertiesConfiguredWhenNotOverridden() {
        contextRunner
                .run(context -> {
                    var loggingExporterType = context.getEnvironment().getProperty(
                            OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type");
                    var metricsExporterType = context.getEnvironment().getProperty(
                            OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type");

                    assertThat(loggingExporterType).isEqualTo("none");
                    assertThat(metricsExporterType).isEqualTo("none");
                });
    }

    @Test
    void customDefaultPropertiesNotConfiguredWhenOverridden() {
        contextRunner
                .withPropertyValues(
                        "arconia.otel.logs.exporter.type=console",
                        "arconia.otel.metrics.exporter.type=console"
                )
                .run(context -> {
                    var loggingExporterType = context.getEnvironment().getProperty(
                            OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type");
                    var metricsExporterType = context.getEnvironment().getProperty(
                            OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type");

                    assertThat(loggingExporterType).isEqualTo("console");
                    assertThat(metricsExporterType).isEqualTo("console");
                });
    }

}
