package io.arconia.opentelemetry.autoconfigure.micrometer.metrics;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.ConditionalOnEnabledOpenTelemetryMetrics;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.micrometer.metrics.MicrometerMetricsBridgeAutoConfiguration.MetricsExportEnabled;

/**
 * Auto-configuration for Micrometer metrics bridge to OpenTelemetry.
 */
@AutoConfiguration(
    after = { MetricsAutoConfiguration.class, OpenTelemetryAutoConfiguration.class },
    before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class }
)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnEnabledOpenTelemetryMetrics
@Conditional(MetricsExportEnabled.class)
public class MicrometerMetricsBridgeAutoConfiguration {

    @Bean
    @ConditionalOnBean({ Clock.class, OpenTelemetry.class })
    MeterRegistry meterRegistry(Clock clock, OpenTelemetry openTelemetry) {
        return OpenTelemetryMeterRegistry.builder(openTelemetry)
                .setPrometheusMode(false)
                .setMicrometerHistogramGaugesEnabled(true)
                .setBaseTimeUnit(TimeUnit.MILLISECONDS)
                .setClock(clock)
                .build();
    }

    /**
     * Condition to check if OpenTelemetry metrics export is enabled.
     * This can be removed after upgrading to Spring Boot 3.5,
     * which supports stacking @ConditionalOnProperty annotations.
     */
    static class MetricsExportEnabled extends AnyNestedCondition {

        MetricsExportEnabled() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "console")
        static class ConsoleMetricsExportEnabled {}

        @ConditionalOnProperty(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "otlp", matchIfMissing = true)
        static class OtlpMetricsExportEnabled {}

    }

}
