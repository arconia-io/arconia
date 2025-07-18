package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

import java.util.concurrent.Executors;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.VirtualThreadTaskExecutor;

import io.arconia.opentelemetry.autoconfigure.metrics.ConditionalOnOpenTelemetryMetrics;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMeterProviderBuilderCustomizer;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.console.ConsoleMetricsExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;

/**
 * Auto-configuration for exporting OpenTelemetry metrics.
 */
@AutoConfiguration(before = org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration.class)
@ConditionalOnOpenTelemetryMetrics
@Import({ ConsoleMetricsExporterConfiguration.class, OtlpMetricsExporterConfiguration.class })
@EnableConfigurationProperties(OpenTelemetryMetricsExporterProperties.class)
public class OpenTelemetryMetricsExporterAutoConfiguration {

    private static final String THREAD_NAME_PREFIX = "otel-metrics";

    @Bean
    @ConditionalOnThreading(Threading.PLATFORM)
    OpenTelemetryMeterProviderBuilderCustomizer metricBuilderPlatformThreads(OpenTelemetryMetricsExporterProperties properties,
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
    OpenTelemetryMeterProviderBuilderCustomizer metricBuilderVirtualThreads(OpenTelemetryMetricsExporterProperties properties,
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

}
