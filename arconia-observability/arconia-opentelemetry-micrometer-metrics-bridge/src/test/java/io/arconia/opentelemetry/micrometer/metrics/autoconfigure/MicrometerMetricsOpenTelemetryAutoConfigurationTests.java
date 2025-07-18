package io.arconia.opentelemetry.micrometer.metrics.autoconfigure;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ReflectionUtils;

import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerMetricsOpenTelemetryAutoConfiguration}.
 */
class MicrometerMetricsOpenTelemetryAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MicrometerMetricsOpenTelemetryAutoConfiguration.class))
            .withBean(Clock.class, () -> Clock.SYSTEM)
            .withBean(OpenTelemetry.class, OpenTelemetry::noop)
            .withBean(OpenTelemetryMetricsProperties.class, () -> {
                OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
                properties.setInterval(Duration.ofSeconds(5));
                return properties;
            });

    @Test
    void autoConfigurationNotActivatedWhenMeterRegistryClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(MeterRegistry.class))
                .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryMeterRegistryClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(OpenTelemetryMeterRegistry.class))
                .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenMetricsDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenBridgeDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.micrometer-bridge.opentelemetry-api.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenMetricsExportDisabled() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.exporter.type=none")
                .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

    @Test
    void meterRegistryAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(OpenTelemetry.class);

            MeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
            assertThat(registry).isNotNull();
            registry = context.getBean(OpenTelemetryMeterRegistry.class);
            assertThat(registry).isNotNull();
        });
    }

    @Test
    void meterRegistryConfiguredWithCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "arconia.otel.metrics.micrometer-bridge.opentelemetry-api.base-time-unit=milliseconds",
                        "arconia.otel.metrics.micrometer-bridge.opentelemetry-api.histogram-gauges=false"
                )
                .run(context -> {
                    MeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
                    assertThat(registry).isNotNull();
                    registry = context.getBean(OpenTelemetryMeterRegistry.class);
                    assertThat(registry).isNotNull();

                    OpenTelemetryMeterRegistry otelRegistry = (OpenTelemetryMeterRegistry) registry;

                    Field baseTimeUnitField = ReflectionUtils.findField(OpenTelemetryMeterRegistry.class, "baseTimeUnit");
                    ReflectionUtils.makeAccessible(baseTimeUnitField);
                    assertThat(ReflectionUtils.getField(baseTimeUnitField, otelRegistry)).isEqualTo(TimeUnit.MILLISECONDS);

                    // Field distributionStatisticConfigModifierField = ReflectionUtils.findField(OpenTelemetryMeterRegistry.class, "distributionStatisticConfigModifier");
                    // ReflectionUtils.makeAccessible(distributionStatisticConfigModifierField);
                    // assertThat(ReflectionUtils.getField(distributionStatisticConfigModifierField, otelRegistry)).isEqualTo(DistributionStatisticConfigModifier.DISABLE_HISTOGRAM_GAUGES);

                    Field namingConventionField = ReflectionUtils.findField(OpenTelemetryMeterRegistry.class, "namingConvention");
                    ReflectionUtils.makeAccessible(namingConventionField);
                    assertThat(ReflectionUtils.getField(namingConventionField, otelRegistry)).isEqualTo(NamingConvention.identity);
                });
    }

    @Test
    void meterRegistryAvailableWithMetricsExporters() {
        // OTLP exporter (default)
        contextRunner.run(context -> {
            MeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
            assertThat(registry).isNotNull();
            registry = context.getBean(OpenTelemetryMeterRegistry.class);
            assertThat(registry).isNotNull();
        });

        // OTLP exporter (explicit)
        contextRunner
                .withPropertyValues("arconia.otel.metrics.exporter.type=otlp")
                .run(context -> {
                    MeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
                    assertThat(registry).isNotNull();
                    registry = context.getBean(OpenTelemetryMeterRegistry.class);
                    assertThat(registry).isNotNull();
                });

        // Console exporter
        contextRunner
                .withPropertyValues("arconia.otel.metrics.exporter.type=console")
                .run(context -> {
                    MeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
                    assertThat(registry).isNotNull();
                    registry = context.getBean(OpenTelemetryMeterRegistry.class);
                    assertThat(registry).isNotNull();
                });
    }

    @Test
    void meterRegistryNotAvailableWhenClockMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MicrometerMetricsOpenTelemetryAutoConfiguration.class))
                .withBean(OpenTelemetry.class, OpenTelemetry::noop)
                .withBean(OpenTelemetryMetricsProperties.class, () -> {
                    OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
                    properties.setInterval(Duration.ofSeconds(5));
                    return properties;
                })
                .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

    @Test
    void meterRegistryNotAvailableWhenOpenTelemetryMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MicrometerMetricsOpenTelemetryAutoConfiguration.class))
                .withBean(Clock.class, () -> Clock.SYSTEM)
                .withBean(OpenTelemetryMetricsProperties.class, () -> {
                    OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
                    properties.setInterval(Duration.ofSeconds(5));
                    return properties;
                })
                .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

}
