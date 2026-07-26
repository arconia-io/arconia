package io.arconia.observation.opentelemetry.autoconfigure;

import java.util.List;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmThreadMeterConventions;

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
    void activatesWhenOnClasspath() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetryJvmMemoryMetrics.class);
            assertThat(context).hasSingleBean(OpenTelemetryJvmMemoryMeterFilter.class);
            assertThat(context).hasSingleBean(OpenTelemetryJvmThreadMeterConventions.class);
            assertThat(context).hasSingleBean(OpenTelemetryJvmClassLoadingMeterConventions.class);
            assertThat(context).hasSingleBean(OpenTelemetryJvmCpuMeterConventions.class);
        });
    }

    @Test
    void doesNotActivateWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.jvm.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMetrics.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMeterFilter.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmThreadMeterConventions.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmClassLoadingMeterConventions.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmCpuMeterConventions.class);
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

    @Test
    void customJvmThreadMeterConventionsTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomJvmThreadMeterConventionsConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(JvmThreadMeterConventions.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmThreadMeterConventions.class);
                });
    }

    @Test
    void customJvmClassLoadingMeterConventionsTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomJvmClassLoadingMeterConventionsConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(JvmClassLoadingMeterConventions.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmClassLoadingMeterConventions.class);
                });
    }

    @Test
    void customJvmCpuMeterConventionsTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomJvmCpuMeterConventionsConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(JvmCpuMeterConventions.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryJvmCpuMeterConventions.class);
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

    @Configuration(proxyBeanMethods = false)
    static class CustomJvmThreadMeterConventionsConfig {
        @Bean
        JvmThreadMeterConventions jvmThreadMeterConventions() {
            return new MicrometerJvmThreadMeterConventions(Tags.empty());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomJvmClassLoadingMeterConventionsConfig {
        @Bean
        JvmClassLoadingMeterConventions jvmClassLoadingMeterConventions() {
            return new MicrometerJvmClassLoadingMeterConventions();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomJvmCpuMeterConventionsConfig {
        @Bean
        JvmCpuMeterConventions jvmCpuMeterConventions() {
            return new MicrometerJvmCpuMeterConventions(Tags.empty());
        }
    }

}
