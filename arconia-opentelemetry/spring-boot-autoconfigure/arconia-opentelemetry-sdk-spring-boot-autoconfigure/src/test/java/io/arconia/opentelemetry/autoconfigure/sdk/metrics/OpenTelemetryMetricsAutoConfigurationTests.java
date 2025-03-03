package io.arconia.opentelemetry.autoconfigure.sdk.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
import io.opentelemetry.sdk.resources.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryMetricsAutoConfiguration}.
 */
class OpenTelemetryMetricsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryMetricsAutoConfiguration.class))
            .withPropertyValues("arconia.otel.enabled=true")
            .withBean(Clock.class, Clock::getDefault)
            .withBean(Resource.class, Resource::empty)
            .withBean(OpenTelemetry.class, () -> mock(OpenTelemetry.class));

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
    void autoConfigurationNotActivatedWhenMeterProviderClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(SdkMeterProvider.class))
                .run(context -> assertThat(context).doesNotHaveBean(SdkMeterProvider.class));
    }

    @Test
    void meterProviderAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SdkMeterProvider.class);
            assertThat(context).hasSingleBean(CardinalityLimitSelector.class);
            assertThat(context).hasSingleBean(ExemplarFilter.class);
            assertThat(context).hasBean("histogramAggregation");
            
            // Verify default histogram aggregation
            OpenTelemetryMetricsProperties properties = context.getBean(OpenTelemetryMetricsProperties.class);
            assertThat(properties.getHistogramAggregation().name())
                .isEqualTo("EXPLICIT_BUCKET_HISTOGRAM");
        });
    }

    @Test
    void exemplarFilterConfigurationApplied() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplar-filter=always-on")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.alwaysOn());
            });

        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplar-filter=always-off")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.alwaysOff());
            });

        contextRunner
            .withPropertyValues("arconia.otel.metrics.exemplar-filter=trace-based")
            .run(context -> {
                ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
                assertThat(exemplarFilter).isEqualTo(ExemplarFilter.traceBased());
            });
    }

    @Test
    void platformThreadsMetricBuilderCustomizerConfigurationApplied() {
        contextRunner
            .withUserConfiguration(CustomMetricExporterConfiguration.class)
            .withPropertyValues(
                "arconia.otel.metrics.interval=10s",
                "spring.threads.virtual.enabled=false"
            )
            .run(context -> {
                assertThat(context.getBeansOfType(SdkMeterProviderBuilderCustomizer.class)).hasSize(2);
                assertThat(context).hasBean("metricBuilderPlatformThreads");
                assertThat(context).doesNotHaveBean("metricBuilderVirtualThreads");
            });
    }

    @Test
    void virtualThreadsMetricBuilderCustomizerConfigurationApplied() {
        contextRunner
            .withUserConfiguration(CustomMetricExporterConfiguration.class)
            .withPropertyValues(
                "arconia.otel.metrics.interval=10s",
                "spring.threads.virtual.enabled=true"
            )
            .run(context -> {
                assertThat(context.getBeansOfType(SdkMeterProviderBuilderCustomizer.class)).hasSize(2);
                assertThat(context).hasBean("metricBuilderVirtualThreads");
                assertThat(context).doesNotHaveBean("metricBuilderPlatformThreads");
            });
    }

    @Test
    void customMetricBuilderCustomizerCoexistsWithAutoConfigured() {
        contextRunner
            .withUserConfiguration(CustomMetricBuilderCustomizerConfiguration.class)
            .withPropertyValues("spring.threads.virtual.enabled=true")
            .run(context -> {
                assertThat(context.getBeansOfType(SdkMeterProviderBuilderCustomizer.class)).hasSize(3);
                assertThat(context).hasBean("customMetricBuilderCustomizer");
                assertThat(context).hasBean("metricBuilderVirtualThreads");
            });

        contextRunner
            .withUserConfiguration(CustomMetricBuilderCustomizerConfiguration.class)
            .withPropertyValues("spring.threads.virtual.enabled=false")
            .run(context -> {
                assertThat(context.getBeansOfType(SdkMeterProviderBuilderCustomizer.class)).hasSize(3);
                assertThat(context).hasBean("customMetricBuilderCustomizer");
                assertThat(context).hasBean("metricBuilderPlatformThreads");
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

    @Test
    void meterAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Meter.class);
        });
    }

    @Test
    void histogramAggregationConfigurationApplied() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.histogram-aggregation=base2-exponential-bucket-histogram")
            .run(context -> {
                assertThat(context.getBeansOfType(SdkMeterProviderBuilderCustomizer.class)).hasSize(2);
                assertThat(context).hasBean("histogramAggregation");
                
                // Verify the histogram aggregation property is set correctly
                OpenTelemetryMetricsProperties properties = context.getBean(OpenTelemetryMetricsProperties.class);
                assertThat(properties.getHistogramAggregation().name())
                    .isEqualTo("BASE2_EXPONENTIAL_BUCKET_HISTOGRAM");
            });

        contextRunner
            .withPropertyValues("arconia.otel.metrics.histogram-aggregation=explicit-bucket-histogram")
            .run(context -> {
                assertThat(context.getBeansOfType(SdkMeterProviderBuilderCustomizer.class)).hasSize(2);
                assertThat(context).hasBean("histogramAggregation");
                
                // Verify the histogram aggregation property is set correctly
                OpenTelemetryMetricsProperties properties = context.getBean(OpenTelemetryMetricsProperties.class);
                assertThat(properties.getHistogramAggregation().name())
                    .isEqualTo("EXPLICIT_BUCKET_HISTOGRAM");
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomMeterProviderConfiguration {

        private final SdkMeterProviderBuilderCustomizer customizer = mock(SdkMeterProviderBuilderCustomizer.class);

        @Bean
        SdkMeterProviderBuilderCustomizer customMeterProviderBuilderCustomizer() {
            return customizer;
        }

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
    static class CustomMetricExporterConfiguration {

        private final MetricExporter customMetricExporter = LoggingMetricExporter.create();

        @Bean
        MetricExporter customMetricExporter() {
            return customMetricExporter;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomMetricBuilderCustomizerConfiguration {

        private final SdkMeterProviderBuilderCustomizer customMetricBuilderCustomizer = mock(SdkMeterProviderBuilderCustomizer.class);

        @Bean
        SdkMeterProviderBuilderCustomizer customMetricBuilderCustomizer() {
            return customMetricBuilderCustomizer;
        }

    }

}
