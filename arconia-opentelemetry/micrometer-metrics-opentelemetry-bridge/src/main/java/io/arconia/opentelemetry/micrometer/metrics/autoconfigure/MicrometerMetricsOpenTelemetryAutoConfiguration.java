package io.arconia.opentelemetry.micrometer.metrics.autoconfigure;

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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import io.arconia.opentelemetry.autoconfigure.sdk.OpenTelemetryAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.ConditionalOnOpenTelemetryMetrics;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.ConditionalOnOpenTelemetryMetricsExporter;

/**
 * Auto-configuration for Micrometer metrics bridge to OpenTelemetry.
 */
@AutoConfiguration(
    after = { MetricsAutoConfiguration.class, OpenTelemetryAutoConfiguration.class },
    before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class }
)
@ConditionalOnClass({MeterRegistry.class, OpenTelemetryMeterRegistry.class})
@ConditionalOnProperty(prefix = MicrometerMetricsOpenTelemetryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnOpenTelemetryMetrics
@Conditional(MicrometerMetricsOpenTelemetryAutoConfiguration.MetricsExportEnabled.class)
@EnableConfigurationProperties(MicrometerMetricsOpenTelemetryProperties.class)
public class MicrometerMetricsOpenTelemetryAutoConfiguration {

    @Bean
    @ConditionalOnBean({ Clock.class, OpenTelemetry.class })
    MeterRegistry meterRegistry(MicrometerMetricsOpenTelemetryProperties properties, Clock clock, OpenTelemetry openTelemetry) {
        return OpenTelemetryMeterRegistry.builder(openTelemetry)
                .setBaseTimeUnit(properties.getBaseTimeUnit())
                .setClock(clock)
                .setMicrometerHistogramGaugesEnabled(properties.isHistogramGauges())
                .setPrometheusMode(false)
                .build();
    }

    @Bean
    public static CompositeMeterRegistryBeanPostProcessor compositeMeterRegistryBeanPostProcessor() {
        return new CompositeMeterRegistryBeanPostProcessor();
    }

    static class MetricsExportEnabled extends AnyNestedCondition {

        MetricsExportEnabled() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnOpenTelemetryMetricsExporter("console")
        static class ConsoleMetricsExportEnabled {}

        @ConditionalOnOpenTelemetryMetricsExporter("otlp")
        static class OtlpMetricsExportEnabled {}

    }

}
