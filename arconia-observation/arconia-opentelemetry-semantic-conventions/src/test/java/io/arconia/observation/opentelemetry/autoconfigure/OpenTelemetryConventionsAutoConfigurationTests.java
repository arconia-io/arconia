package io.arconia.observation.opentelemetry.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryConventionsAutoConfiguration}.
 */
class OpenTelemetryConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryConventionsAutoConfiguration.class));

    @Test
    void autoActivatesWhenOnClasspath() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(OpenTelemetryResourceBuilderCustomizer.class));
    }

    @Test
    void noResourceBuilderCustomizerWhenNotInClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(OpenTelemetryResourceBuilderCustomizer.class))
                .run(context ->
                        assertThat(context).doesNotHaveBean(OpenTelemetryResourceBuilderCustomizer.class));
    }

    @Test
    void observationGroupsEnabledByDefault() {
        contextRunner.run(context -> {
            OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
            assertThat(properties.getHttp().isEnabled()).isTrue();
            assertThat(properties.getJvm().isEnabled()).isTrue();
        });
    }

    @Test
    void bindsEnabledProperties() {
        contextRunner
                .withPropertyValues(
                        "arconia.observations.conventions.opentelemetry.http.enabled=false",
                        "arconia.observations.conventions.opentelemetry.jvm.enabled=false"
                )
                .run(context -> {
                    OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
                    assertThat(properties.getHttp().isEnabled()).isFalse();
                    assertThat(properties.getJvm().isEnabled()).isFalse();
                });
    }

}
