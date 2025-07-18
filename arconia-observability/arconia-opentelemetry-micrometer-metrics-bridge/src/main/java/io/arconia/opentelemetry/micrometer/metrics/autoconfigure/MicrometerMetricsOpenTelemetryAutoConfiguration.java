package io.arconia.opentelemetry.micrometer.metrics.autoconfigure;

import java.time.Duration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.CountingMode;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

import io.arconia.opentelemetry.autoconfigure.metrics.ConditionalOnOpenTelemetryMetrics;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.ConditionalOnOpenTelemetryMetricsExporter;

/**
 * Auto-configuration for Micrometer metrics bridge to OpenTelemetry.
 */
@AutoConfiguration(
    after = { MetricsAutoConfiguration.class, OpenTelemetryMetricsAutoConfiguration.class },
    before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class }
)
@ConditionalOnClass({MeterRegistry.class, OpenTelemetryMeterRegistry.class})
@ConditionalOnProperty(prefix = MicrometerMetricsOpenTelemetryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnOpenTelemetryMetrics
@Conditional(MicrometerMetricsOpenTelemetryAutoConfiguration.MetricsExportEnabled.class)
@EnableConfigurationProperties(MicrometerMetricsOpenTelemetryProperties.class)
public class MicrometerMetricsOpenTelemetryAutoConfiguration {

    // A MeterRegistry used exclusively for reading metrics, e.g. from the Actuator /metrics endpoint.
    // This is necessary because the OpenTelemetryMeterRegistry doesn't support reading metrics, but
    // only bridging them to OpenTelemetry. We register this first so that it is the default
    // MeterRegistry used by the Actuator.
    @Bean
    @ConditionalOnBean({ Clock.class, OpenTelemetry.class, OpenTelemetryMetricsProperties.class })
    public SimpleMeterRegistry simpleMeterRegistry(Clock clock, OpenTelemetryMetricsProperties openTelemetryMetricsProperties) {
        return new SimpleMeterRegistry(new OpenTelemetrySimpleConfig(openTelemetryMetricsProperties), clock);
    }

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

    static class MetricsExportEnabled extends AnyNestedCondition {

        MetricsExportEnabled() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnOpenTelemetryMetricsExporter("console")
        static class ConsoleMetricsExportEnabled {}

        @ConditionalOnOpenTelemetryMetricsExporter("otlp")
        static class OtlpMetricsExportEnabled {}

    }

    static class OpenTelemetrySimpleConfig implements SimpleConfig {

        private final OpenTelemetryMetricsProperties openTelemetryMetricsProperties;

        OpenTelemetrySimpleConfig(OpenTelemetryMetricsProperties openTelemetryMetricsProperties) {
            this.openTelemetryMetricsProperties = openTelemetryMetricsProperties;
        }

        @Override
        public String get(String key) {
            return "";
        }

        @Override
        public Duration step() {
            return openTelemetryMetricsProperties.getInterval();
        }

        @Override
        public CountingMode mode() {
            return CountingMode.CUMULATIVE;
        }

    }

}
