package io.arconia.observation.opentelemetry.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.observation.conventions.ObservationConventionsProvider;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryGenAiOptions;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryConventionsAutoConfiguration}.
 */
class OpenTelemetryConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryConventionsAutoConfiguration.class));

    @Test
    void autoActivatesWhenConventionTypePropertyNotSet() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ObservationConventionsProvider.class);
            assertThat(context.getBean(ObservationConventionsProvider.class).name()).isEqualTo("opentelemetry");
            assertThat(context).hasSingleBean(OpenTelemetryResourceBuilderCustomizer.class);
        });
    }

    @Test
    void activatesWhenConventionTypeExplicitlySetToOpenTelemetry() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.type=opentelemetry")
                .run(context ->
                        assertThat(context).hasSingleBean(ObservationConventionsProvider.class));
    }

    @Test
    void doesNotActivateWhenConventionTypeSetToDifferentValue() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.type=micrometer")
                .run(context ->
                        assertThat(context).doesNotHaveBean(ObservationConventionsProvider.class));
    }

    @Test
    void noResourceBuilderCustomizerWhenNotInClassapth() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(OpenTelemetryResourceBuilderCustomizer.class))
                .run(context ->
                        assertThat(context).doesNotHaveBean(OpenTelemetryResourceBuilderCustomizer.class));
    }

    @Test
    void bindsProperties() {
        contextRunner
                .withPropertyValues(
                        "arconia.observations.conventions.opentelemetry.generative-ai.inference.capture-content=span-attributes",
                        "arconia.observations.conventions.opentelemetry.generative-ai.inference.include-tool-definitions=true",
                        "arconia.observations.conventions.opentelemetry.generative-ai.tool-execution.include-content=true"
                )
                .run(context -> {
                    OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
                    assertThat(properties.getGenerativeAi().getInference().getCaptureContent())
                            .isEqualTo(OpenTelemetryGenAiOptions.CaptureContentFormat.SPAN_ATTRIBUTES);
                    assertThat(properties.getGenerativeAi().getInference().isIncludeToolDefinitions()).isTrue();
                    assertThat(properties.getGenerativeAi().getToolExecution().isIncludeContent()).isTrue();
                });
    }

    @Test
    void propertiesDefaultToSecureValues() {
        contextRunner.run(context -> {
            OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
            assertThat(properties.getGenerativeAi().getInference().getCaptureContent())
                    .isEqualTo(OpenTelemetryGenAiOptions.CaptureContentFormat.NONE);
            assertThat(properties.getGenerativeAi().getInference().isIncludeToolDefinitions()).isFalse();
            assertThat(properties.getGenerativeAi().getToolExecution().isIncludeContent()).isFalse();
        });
    }

    @Test
    void observationGroupsEnabledByDefault() {
        contextRunner.run(context -> {
            OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
            assertThat(properties.getGenerativeAi().isEnabled()).isTrue();
            assertThat(properties.getHttp().isEnabled()).isTrue();
            assertThat(properties.getJvm().isEnabled()).isTrue();
        });
    }

    @Test
    void bindsEnabledProperties() {
        contextRunner
                .withPropertyValues(
                        "arconia.observations.conventions.opentelemetry.generative-ai.enabled=false",
                        "arconia.observations.conventions.opentelemetry.http.enabled=false",
                        "arconia.observations.conventions.opentelemetry.jvm.enabled=false"
                )
                .run(context -> {
                    OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
                    assertThat(properties.getGenerativeAi().isEnabled()).isFalse();
                    assertThat(properties.getHttp().isEnabled()).isFalse();
                    assertThat(properties.getJvm().isEnabled()).isFalse();
                });
    }

}
