package io.arconia.opentelemetry.autoconfigure.instrumentation.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.NamingConvention;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerInstrumentationAutoConfiguration}.
 */
class MicrometerInstrumentationAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MicrometerInstrumentationAutoConfiguration.class))
            .withBean(Clock.class, () -> Clock.SYSTEM)
            .withBean(OpenTelemetry.class, () -> OpenTelemetry.noop());

    @Test
    void autoConfigurationNotActivatedWhenMeterRegistryClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(MeterRegistry.class))
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
    void autoConfigurationNotActivatedWhenInstrumentationDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.instrumentation.micrometer.enabled=false")
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
            assertThat(context).hasSingleBean(MeterRegistry.class);
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(OpenTelemetry.class);

            MeterRegistry registry = context.getBean(MeterRegistry.class);
            assertThat(registry).isInstanceOf(OpenTelemetryMeterRegistry.class);
        });
    }

    @Test
    void meterRegistryConfiguredWithCustomProperties() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.instrumentation.micrometer.base-time-unit=seconds",
                "arconia.otel.instrumentation.micrometer.histogram-gauges=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(MeterRegistry.class);
                MeterRegistry registry = context.getBean(MeterRegistry.class);
                assertThat(registry).isInstanceOf(OpenTelemetryMeterRegistry.class);

                OpenTelemetryMeterRegistry otelRegistry = (OpenTelemetryMeterRegistry) registry;
                
                Field baseTimeUnitField = ReflectionUtils.findField(OpenTelemetryMeterRegistry.class, "baseTimeUnit");
                ReflectionUtils.makeAccessible(baseTimeUnitField);
                assertThat(ReflectionUtils.getField(baseTimeUnitField, otelRegistry)).isEqualTo(TimeUnit.SECONDS);

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
        contextRunner.run(context -> assertThat(context).hasSingleBean(MeterRegistry.class));

        // OTLP exporter (explicit)
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=otlp")
            .run(context -> assertThat(context).hasSingleBean(MeterRegistry.class));

        // Console exporter
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=console")
            .run(context -> assertThat(context).hasSingleBean(MeterRegistry.class));
    }

    @Test
    void meterRegistryNotAvailableWhenClockMissing() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MicrometerInstrumentationAutoConfiguration.class))
            .withBean(OpenTelemetry.class, () -> OpenTelemetry.noop())
            .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

    @Test
    void meterRegistryNotAvailableWhenOpenTelemetryMissing() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MicrometerInstrumentationAutoConfiguration.class))
            .withBean(Clock.class, () -> Clock.SYSTEM)
            .run(context -> assertThat(context).doesNotHaveBean(MeterRegistry.class));
    }

}
