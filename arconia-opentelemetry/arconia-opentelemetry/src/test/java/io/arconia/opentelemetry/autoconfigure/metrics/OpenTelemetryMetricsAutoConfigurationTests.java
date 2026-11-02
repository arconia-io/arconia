package io.arconia.opentelemetry.autoconfigure.metrics;

import java.util.concurrent.atomic.AtomicInteger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.ExemplarFilter;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryMetricsAutoConfiguration}.
 */
class OpenTelemetryMetricsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryMetricsAutoConfiguration.class))
            .withBean(Clock.class, Clock::getDefault)
            .withBean(Resource.class, Resource::empty)
            .withBean(OpenTelemetry.class, OpenTelemetry::noop);

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkMeterProvider.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenMetricsDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkMeterProvider.class));
    }

    @Test
    void meterUsesSpringBootInstrumentationScope() {
        InMemoryMetricReader metricReader = InMemoryMetricReader.create();
        try (OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(metricReader).build())
                .build()) {
            Meter meter = new OpenTelemetryMetricsAutoConfiguration().meter(openTelemetry);
            meter.counterBuilder("test.counter").build().add(1);

            InstrumentationScopeInfo scope = metricReader.collectAllMetrics().iterator().next().getInstrumentationScopeInfo();
            assertThat(scope.getName()).isEqualTo(OpenTelemetryMetricsAutoConfiguration.INSTRUMENTATION_SCOPE_NAME);
            assertThat(scope.getVersion()).isEqualTo(SpringBootVersion.getVersion());
        }
    }

    @Test
    void exemplarsRecordedForSampledSpansByDefault() {
        assertThat(exemplarCount()).isEqualTo(1);
    }

    @Test
    void exemplarsDisabledWithAlwaysOffFilter() {
        assertThat(exemplarCount("arconia.otel.metrics.exemplars.filter=always-off")).isZero();
    }

    @Test
    void meterAvailableWhenConfiguredBeforeOpenTelemetry() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class,
                        OpenTelemetryMetricsAutoConfiguration.class, MetricsBeforeOpenTelemetryAutoConfiguration.class))
                .withBean(Resource.class, Resource::empty)
                .run(context -> assertThat(context).hasSingleBean(Meter.class));
    }

    @Test
    void meterProviderAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SdkMeterProvider.class);
            assertThat(context).hasSingleBean(CardinalityLimitSelector.class);
            assertThat(context).hasSingleBean(ExemplarFilter.class);
            assertThat(context).hasSingleBean(Meter.class);
        });
    }

    @Test
    void cardinalityLimitSelectorConfigurationApplied() {
        contextRunner
                .withPropertyValues("arconia.otel.metrics.cardinality-limit=200")
                .run(context -> {
                    CardinalityLimitSelector cardinalityLimitSelector = context.getBean(CardinalityLimitSelector.class);
                    assertThat(cardinalityLimitSelector.getCardinalityLimit(InstrumentType.COUNTER)).isEqualTo(200);
                });
    }

    @Test
    void exemplarFilterConfigurationApplied() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplars.filter=always-on")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.alwaysOn());
            });

        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplars.filter=always-off")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.alwaysOff());
            });

        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplars.filter=trace-based")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.traceBased());
            });
    }

    @Test
    void customCardinalityLimitSelectorAvailable() {
        contextRunner
            .withUserConfiguration(CustomCardinalityLimitSelectorConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(CardinalityLimitSelector.class);
                assertThat(context.getBean(CardinalityLimitSelector.class))
                    .isSameAs(context.getBean(CustomCardinalityLimitSelectorConfiguration.class).customCardinalityLimitSelector());
            });
    }

    /**
     * Records a measurement in the context of a sampled span and counts the exemplars collected.
     */
    private int exemplarCount(String... properties) {
        InMemoryMetricReader metricReader = InMemoryMetricReader.create();
        AtomicInteger exemplarCount = new AtomicInteger();
        contextRunner
            .withPropertyValues(properties)
            .withBean(OpenTelemetryMeterProviderBuilderCustomizer.class, () -> builder -> builder.registerMetricReader(metricReader))
            .run(context -> {
                SdkMeterProvider meterProvider = context.getBean(SdkMeterProvider.class);
                SpanContext spanContext = SpanContext.create("0af7651916cd43dd8448eb211c80319c", "b7ad6b7169203331",
                        TraceFlags.getSampled(), TraceState.getDefault());
                try (Scope ignored = Span.wrap(spanContext).makeCurrent()) {
                    meterProvider.get("test").histogramBuilder("test.duration").build().record(1.0);
                }
                metricReader.collectAllMetrics().forEach(metric ->
                        metric.getHistogramData().getPoints().forEach(point -> exemplarCount.addAndGet(point.getExemplars().size())));
            });
        return exemplarCount.get();
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomCardinalityLimitSelectorConfiguration {

        private final CardinalityLimitSelector customCardinalityLimitSelector = mock(CardinalityLimitSelector.class);

        @Bean
        CardinalityLimitSelector customCardinalityLimitSelector() {
            return customCardinalityLimitSelector;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomMetricBuilderCustomizerConfiguration {

        private final OpenTelemetryMeterProviderBuilderCustomizer customMetricBuilderCustomizer = mock(OpenTelemetryMeterProviderBuilderCustomizer.class);

        @Bean
        OpenTelemetryMeterProviderBuilderCustomizer customMetricBuilderCustomizer() {
            return customMetricBuilderCustomizer;
        }

    }

    /**
     * Orders the metrics auto-configuration before the OpenTelemetry one, as other
     * auto-configurations ordered after both can do.
     */
    @AutoConfiguration(after = OpenTelemetryMetricsAutoConfiguration.class, before = OpenTelemetryAutoConfiguration.class)
    static class MetricsBeforeOpenTelemetryAutoConfiguration {

    }

}
