package io.arconia.observation.opentelemetry.autoconfigure;

import java.util.List;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmThreadMeterConventions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import io.arconia.observation.opentelemetry.instrumentation.jvm.OpenTelemetryJvmMemoryMeterFilter;
import io.arconia.observation.opentelemetry.instrumentation.jvm.OpenTelemetryJvmMemoryMetrics;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions for JVM metrics.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/runtime/jvm-metrics/">OpenTelemetry Semantic Conventions for JVM metrics</a>
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration"
})
@ConditionalOnBooleanProperty(prefix = OpenTelemetryConventionsProperties.CONFIG_PREFIX, value = "jvm.enabled", matchIfMissing = true)
public final class JvmConventionsAutoConfiguration {

    /**
     * Can't rely on upstream OpenTelemetryJvmMemoryMeterConventions directly,
     * because some OTel metrics are not registered there.
     */
    @Bean
    @ConditionalOnMissingBean(JvmMemoryMetrics.class)
    OpenTelemetryJvmMemoryMetrics jvmMemoryMetrics() {
        JvmMemoryMeterConventions conventions = new OpenTelemetryJvmMemoryMeterConventions(Tags.empty());
        return new OpenTelemetryJvmMemoryMetrics(List.of(), conventions);
    }

    @Bean
    OpenTelemetryJvmMemoryMeterFilter openTelemetryJvmMemoryMeterFilter() {
        return new OpenTelemetryJvmMemoryMeterFilter();
    }

    @Bean
    @ConditionalOnMissingBean(JvmThreadMeterConventions.class)
    OpenTelemetryJvmThreadMeterConventions jvmThreadMeterConventions() {
        return new OpenTelemetryJvmThreadMeterConventions(Tags.empty());
    }

    @Bean
    @ConditionalOnMissingBean(JvmClassLoadingMeterConventions.class)
    OpenTelemetryJvmClassLoadingMeterConventions classLoadingMeterConventions() {
        return new OpenTelemetryJvmClassLoadingMeterConventions();
    }

    @Bean
    @ConditionalOnMissingBean(JvmCpuMeterConventions.class)
    OpenTelemetryJvmCpuMeterConventions jmcCpuMeterConventions() {
        return new OpenTelemetryJvmCpuMeterConventions(Tags.empty());
    }

}
