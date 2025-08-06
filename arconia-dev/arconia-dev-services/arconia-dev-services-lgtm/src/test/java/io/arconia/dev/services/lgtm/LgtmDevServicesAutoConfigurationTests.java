package io.arconia.dev.services.lgtm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LgtmDevServicesAutoConfiguration}.
 */
class LgtmDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(LgtmDevServicesAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.lgtm.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(LgtmStackContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryMissing() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(LgtmStackContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(LgtmStackContainer.class);
                    LgtmStackContainer container = context.getBean(LgtmStackContainer.class);
                    assertThat(container.getDockerImageName()).contains("grafana/otel-lgtm");
                    assertThat(container.isShouldBeReused()).isTrue();
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(LgtmStackContainer.class);
                    LgtmStackContainer container = context.getBean(LgtmStackContainer.class);
                    assertThat(container.getDockerImageName()).contains("grafana/otel-lgtm");
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.lgtm.image-name=docker.io/grafana/otel-lgtm",
                "arconia.dev.services.lgtm.environment.ENABLE_LOGS_ALL=true",
                "arconia.dev.services.lgtm.shared=never"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(LgtmStackContainer.class);
                LgtmStackContainer container = context.getBean(LgtmStackContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/grafana/otel-lgtm");
                assertThat(container.getEnv()).contains("ENABLE_LOGS_ALL=true");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
