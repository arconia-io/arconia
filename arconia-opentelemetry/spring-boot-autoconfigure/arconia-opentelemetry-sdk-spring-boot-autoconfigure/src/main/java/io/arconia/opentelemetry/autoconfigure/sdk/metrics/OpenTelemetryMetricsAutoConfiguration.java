package io.arconia.opentelemetry.autoconfigure.sdk.metrics;

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
import io.opentelemetry.sdk.metrics.internal.SdkMeterProviderUtil;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
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
@ConditionalOnOpenTelemetryMetrics
@EnableConfigurationProperties(OpenTelemetryMetricsProperties.class)
public class OpenTelemetryMetricsAutoConfiguration {

    public static final String INSTRUMENTATION_SCOPE_NAME = "org.springframework.boot";

    private static final String THREAD_NAME_PREFIX = "otel-metrics";

    @Bean
    @ConditionalOnMissingBean
    SdkMeterProvider otelSdkMeterProvider(Clock clock,
                                          ExemplarFilter exemplarFilter,
                                          Resource resource,
                                          ObjectProvider<OpenTelemetryMeterProviderBuilderCustomizer> customizers
    ) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder()
                .setClock(clock)
                .setResource(resource);
        SdkMeterProviderUtil.setExemplarFilter(builder, exemplarFilter); // Still experimental, so we need to use the internal utility method.
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    CardinalityLimitSelector cardinalityLimitSelector(OpenTelemetryMetricsProperties properties) {
        return instrumentType -> properties.getCardinalityLimit();
    }

    @Bean
    @ConditionalOnMissingBean
    ExemplarFilter exemplarFilter(OpenTelemetryMetricsProperties properties) {
        return switch(properties.getExemplarFilter()) {
            case ALWAYS_ON -> ExemplarFilter.alwaysOn();
            case ALWAYS_OFF -> ExemplarFilter.alwaysOff();
            case TRACE_BASED -> ExemplarFilter.traceBased();
        };
    }

    @Bean
    @ConditionalOnThreading(Threading.PLATFORM)
    OpenTelemetryMeterProviderBuilderCustomizer metricBuilderPlatformThreads(OpenTelemetryMetricsProperties properties,
                                                                   CardinalityLimitSelector cardinalityLimitSelector,
                                                                   ObjectProvider<MetricExporter> metricExporters
    ) {
        NamedThreadFactory threadFactory = new NamedThreadFactory(THREAD_NAME_PREFIX);
        return builder -> {
            metricExporters.orderedStream().forEach(metricExporter ->
                    builder.registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                            .setInterval(properties.getInterval())
                            .setExecutor(Executors.newSingleThreadScheduledExecutor(threadFactory))
                            .build(), cardinalityLimitSelector));
        };
    }

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    OpenTelemetryMeterProviderBuilderCustomizer metricBuilderVirtualThreads(OpenTelemetryMetricsProperties properties,
                                                      CardinalityLimitSelector cardinalityLimitSelector,
                                                      ObjectProvider<MetricExporter> metricExporters
    ) {
        VirtualThreadTaskExecutor taskExecutor = new VirtualThreadTaskExecutor(THREAD_NAME_PREFIX + "-");
        return builder -> {
            metricExporters.orderedStream().forEach(metricExporter ->
                    builder.registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                            .setInterval(properties.getInterval())
                            .setExecutor(Executors.newSingleThreadScheduledExecutor(taskExecutor.getVirtualThreadFactory()))
                            .build(), cardinalityLimitSelector));
        };
    }

    @Bean
    @ConditionalOnMissingBean
    Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter(INSTRUMENTATION_SCOPE_NAME);
    }

}
