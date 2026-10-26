package io.arconia.observation.opentelemetry.autoconfigure;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.SchemaUrls;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ReflectionUtils;

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
    void setsSchemaUrlOfSemanticConventionsVersion() {
        contextRunner.run(context -> {
            ResourceBuilder builder = Resource.builder();
            context.getBean(OpenTelemetryResourceBuilderCustomizer.class).customize(builder);
            assertThat(builder.build().getSchemaUrl()).isEqualTo(latestSchemaUrl());
        });
    }

    @Test
    void observationGroupsEnabledByDefault() {
        contextRunner.run(context -> {
            OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
            assertThat(properties.getHttp().isEnabled()).isTrue();
            assertThat(properties.getJvm().isEnabled()).isTrue();
            assertThat(properties.getError().isEnabled()).isTrue();
        });
    }

    @Test
    void bindsEnabledProperties() {
        contextRunner
                .withPropertyValues(
                        "arconia.observations.conventions.opentelemetry.http.enabled=false",
                        "arconia.observations.conventions.opentelemetry.jvm.enabled=false",
                        "arconia.observations.conventions.opentelemetry.error.enabled=false"
                )
                .run(context -> {
                    OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
                    assertThat(properties.getHttp().isEnabled()).isFalse();
                    assertThat(properties.getJvm().isEnabled()).isFalse();
                    assertThat(properties.getError().isEnabled()).isFalse();
                });
    }

    /**
     * Returns the schema URL of the latest version in the {@code opentelemetry-semconv} dependency,
     * so that upgrading the dependency without updating the schema URL makes the test fail.
     */
    private static String latestSchemaUrl() {
        Field latest = Arrays.stream(SchemaUrls.class.getFields())
                .filter(field -> field.getName().matches("V\\d+_\\d+_\\d+"))
                .max(Comparator.comparing((Field field) -> version(field, 0))
                        .thenComparing(field -> version(field, 1))
                        .thenComparing(field -> version(field, 2)))
                .orElseThrow();
        return (String) ReflectionUtils.getField(latest, null);
    }

    private static int version(Field field, int index) {
        return Integer.parseInt(field.getName().substring(1).split("_")[index]);
    }

}
