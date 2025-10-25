package io.arconia.openinference.observation.autoconfigure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.ResourceContributor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceResourceConfiguration}.
 */
class OpenInferenceResourceConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenInferenceResourceConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryResourceAutoConfigurationClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(OpenTelemetryResourceAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenInferenceResourceContributor.class);
                });
    }

    @Test
    void autoConfigurationNotActivatedWhenResourceContributorClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(ResourceContributor.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenInferenceResourceContributor.class);
                });
    }

    @Test
    void resourceContributorAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenInferenceResourceContributor.class);
        });
    }

}
