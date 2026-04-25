package io.arconia.observation.opentelemetry.autoconfigure;

import java.util.List;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.opentelemetry.instrumentation.jvm.OpenTelemetryJvmMemoryMeterFilter;
import io.arconia.observation.opentelemetry.instrumentation.jvm.OpenTelemetryJvmMemoryMetrics;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JvmConventionsAutoConfiguration}.
 */
class JvmConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JvmConventionsAutoConfiguration.class));

    // Activation / deactivation

    @Test
    void activatesWhenConventionTypePropertyNotSet() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetryJvmMemoryMetrics.class);
            assertThat(context).hasSingleBean(OpenTelemetryJvmMemoryMeterFilter.class);
            assertThat(context).hasSingleBean(JvmThreadMetrics.class);
            assertThat(context).hasSingleBean(ClassLoaderMetrics.class);
            assertThat(context).hasSingleBean(ProcessorMetrics.class);
        });
    }

    @Test
    void activatesWhenConventionTypeExplicitlySetToOpenTelemetry() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.type=opentelemetry")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenTelemetryJvmMemoryMetrics.class);
                    assertThat(context).hasSingleBean(OpenTelemetryJvmMemoryMeterFilter.class);
                });
    }

    @Test
    void doesNotActivateWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.jvm.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMetrics.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMeterFilter.class);
                    assertThat(context).doesNotHaveBean(JvmThreadMetrics.class);
                    assertThat(context).doesNotHaveBean(ClassLoaderMetrics.class);
                    assertThat(context).doesNotHaveBean(ProcessorMetrics.class);
                });
    }

    @Test
    void doesNotActivateWhenConventionTypeSetToDifferentValue() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.type=micrometer")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMetrics.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMeterFilter.class);
                    assertThat(context).doesNotHaveBean(JvmThreadMetrics.class);
                    assertThat(context).doesNotHaveBean(ClassLoaderMetrics.class);
                    assertThat(context).doesNotHaveBean(ProcessorMetrics.class);
                });
    }

    // Custom bean precedence

    @Test
    void customJvmMemoryMetricsTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomJvmMemoryMetricsConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(JvmMemoryMetrics.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMetrics.class);
                });
    }

    // Custom bean configurations

    @Configuration(proxyBeanMethods = false)
    static class CustomJvmMemoryMetricsConfig {
        @Bean
        JvmMemoryMetrics jvmMemoryMetrics() {
            return new JvmMemoryMetrics(List.of(),
                    new OpenTelemetryJvmMemoryMeterConventions(Tags.empty()));
        }
    }

}
