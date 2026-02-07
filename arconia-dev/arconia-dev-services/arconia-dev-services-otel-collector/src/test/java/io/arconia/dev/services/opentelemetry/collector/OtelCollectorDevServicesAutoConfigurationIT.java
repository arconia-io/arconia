package io.arconia.dev.services.opentelemetry.collector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtelCollectorDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OtelCollectorDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(OtelCollectorDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArconiaOtelCollectorContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.otel-collector.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArconiaOtelCollectorContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArconiaOtelCollectorContainer.class));
    }

    @Test
    void containerAvailableInDevMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(ArconiaOtelCollectorContainer.class);
                    ArconiaOtelCollectorContainer container = context.getBean(ArconiaOtelCollectorContainer.class);
                    assertThat(container.getDockerImageName()).contains("otel/opentelemetry-collector-contrib");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isTrue();

                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(ArconiaOtelCollectorContainer.class);
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
                    assertThat(context).hasSingleBean(ArconiaOtelCollectorContainer.class);
                    ArconiaOtelCollectorContainer container = context.getBean(ArconiaOtelCollectorContainer.class);
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.otel-collector.environment.KEY=value",
                        "arconia.dev.services.otel-collector.network-aliases=network1",
                        "arconia.dev.services.otel-collector.resources[0].source-path=test-resource.txt",
                        "arconia.dev.services.otel-collector.resources[0].container-path=/tmp/test-resource.txt"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ArconiaOtelCollectorContainer.class);
                    ArconiaOtelCollectorContainer container = context.getBean(ArconiaOtelCollectorContainer.class);
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
                    assertThat(context).hasSingleBean(ArconiaOtelCollectorContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(ArconiaOtelCollectorContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
