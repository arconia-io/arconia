package io.arconia.observation.opentelemetry.autoconfigure;

import java.util.List;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.arconia.observation.autoconfigure.ObservationProperties;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions for JVM metrics.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/runtime/jvm-metrics/">OpenTelemetry Semantic Conventions for JVM metrics</a>
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration"
})
@ConditionalOnProperty(prefix = ObservationProperties.CONFIG_PREFIX, name = "conventions.type", havingValue = "opentelemetry", matchIfMissing = true)
@ConditionalOnBooleanProperty(prefix = OpenTelemetryConventionsProperties.CONFIG_PREFIX, value = "jvm.enabled", matchIfMissing = true)
public final class JvmConventionsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    JvmMemoryMetrics jvmMemoryMetrics() {
        JvmMemoryMeterConventions conventions = new OpenTelemetryJvmMemoryMeterConventions(Tags.empty());
        return new JvmMemoryMetrics(List.of(), conventions);
    }

    @Bean
    @ConditionalOnMissingBean
    JvmThreadMetrics jvmThreadMetrics() {
        JvmThreadMeterConventions conventions = new OpenTelemetryJvmThreadMeterConventions(Tags.empty());
        return new JvmThreadMetrics(List.of(), conventions);
    }

    @Bean
    @ConditionalOnMissingBean
    ClassLoaderMetrics classLoaderMetrics() {
        JvmClassLoadingMeterConventions conventions = new OpenTelemetryJvmClassLoadingMeterConventions();
        return new ClassLoaderMetrics(conventions);
    }

    @Bean
    @ConditionalOnMissingBean
    ProcessorMetrics processorMetrics() {
        JvmCpuMeterConventions conventions = new OpenTelemetryJvmCpuMeterConventions(Tags.empty());
        return new ProcessorMetrics(List.of(), conventions);
    }

}
