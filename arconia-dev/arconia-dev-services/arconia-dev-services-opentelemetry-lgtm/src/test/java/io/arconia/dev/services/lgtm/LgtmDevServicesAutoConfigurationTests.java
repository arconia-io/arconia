package io.arconia.dev.services.lgtm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.grafana.LgtmStackContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LgtmDevServicesAutoConfiguration}.
 */
class LgtmDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(LgtmDevServicesAutoConfiguration.class));

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
    void lgtmContainerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LgtmStackContainer.class);
            LgtmStackContainer container = context.getBean(LgtmStackContainer.class);
            assertThat(container.getDockerImageName()).contains("grafana/otel-lgtm");
            assertThat(container.isShouldBeReused()).isTrue();
        });
    }

    @Test
    void lgtmContainerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.lgtm.image-name=grafana/otel-lgtm:0.9.1",
                "arconia.dev.services.lgtm.reusable=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(LgtmStackContainer.class);
                LgtmStackContainer container = context.getBean(LgtmStackContainer.class);
                assertThat(container.getDockerImageName()).contains("grafana/otel-lgtm:0.9.1");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
