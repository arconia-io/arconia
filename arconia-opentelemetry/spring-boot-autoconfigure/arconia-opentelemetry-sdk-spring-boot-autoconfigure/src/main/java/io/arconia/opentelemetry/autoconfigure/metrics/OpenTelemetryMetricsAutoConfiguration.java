package io.arconia.opentelemetry.autoconfigure.metrics;

import java.util.concurrent.Executors;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * Auto-configuration for OpenTelemetry metrics.
 */
@AutoConfiguration
@ConditionalOnClass(SdkMeterProvider.class)
@ConditionalOnEnabledOpenTelemetryMetrics
@EnableConfigurationProperties(OpenTelemetryMetricsProperties.class)
public class OpenTelemetryMetricsAutoConfiguration {

    private static final String THREAD_NAME_PREFIX = "otel-metrics-publisher";

    @Bean
    @ConditionalOnMissingBean
    SdkMeterProvider otelSdkMeterProvider(Clock clock,
                                          Resource resource,
                                          ObjectProvider<SdkMeterProviderBuilderCustomizer> customizers
    ) {
        // TODO: Configure ExemplarFilter when it becomes GA
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder()
                .setClock(clock)
                .setResource(resource);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnThreading(Threading.PLATFORM)
    SdkMeterProviderBuilderCustomizer metricReaderPlatformThreads(OpenTelemetryMetricsProperties properties,
                                                                  CardinalityLimitSelector cardinalityLimitSelector,
                                                                  ObjectProvider<MetricExporter> metricExporters
    ) {
        NamedThreadFactory threadFactory = new NamedThreadFactory(THREAD_NAME_PREFIX);
        return builder -> metricExporters.orderedStream().forEach(exporter -> builder
                .registerMetricReader(
                    PeriodicMetricReader.builder(exporter)
                            .setInterval(properties.getInterval())
                            .setExecutor(Executors.newSingleThreadScheduledExecutor(threadFactory))
                            .build(),
                    cardinalityLimitSelector
                ));
    }

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    SdkMeterProviderBuilderCustomizer metricReaderVirtualThreads(OpenTelemetryMetricsProperties properties,
                                                                 CardinalityLimitSelector cardinalityLimitSelector,
                                                                 ObjectProvider<MetricExporter> metricExporters
    ) {
        VirtualThreadTaskExecutor taskExecutor = new VirtualThreadTaskExecutor(THREAD_NAME_PREFIX + "-");
        return builder -> metricExporters.orderedStream().forEach(exporter -> builder
                .registerMetricReader(
                    PeriodicMetricReader.builder(exporter)
                            .setInterval(properties.getInterval())
                            .setExecutor(Executors.newSingleThreadScheduledExecutor(taskExecutor.getVirtualThreadFactory()))
                            .build(),
                    cardinalityLimitSelector
                ));
    }

    @Bean
    @ConditionalOnMissingBean
    CardinalityLimitSelector cardinalityLimitSelector() {
        return CardinalityLimitSelector.defaultCardinalityLimitSelector();
    }

    @Bean
    @ConditionalOnMissingBean
    Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter("org.springframework.boot");
    }

}
