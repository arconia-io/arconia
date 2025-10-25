package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.HistogramFlavor;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.VirtualThreadTaskExecutor;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.ConditionalOnOpenTelemetryMetricsExporter;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceAutoConfiguration;

/**
 * Auto-configuration for Micrometer Registry OTLP which pushes metrics to an OpenTelemetry backend.
 * This is different from the Micrometer Metrics Bridge as this doesn't use the OpenTelemetry Metrics API.
 * Instead, it uses the underlying OTLP protocol to push metrics. As a result, two different exporters are used:
 * one for metrics instrumented via OpenTelemetry API and one for metrics instrumented via Micrometer API.
 * If you want to use a single exporter for both, use the Micrometer Metrics Bridge.
 */
@AutoConfiguration(
    after = {MetricsAutoConfiguration.class, OpenTelemetryMetricsAutoConfiguration.class, OpenTelemetryMetricsExporterAutoConfiguration.class, OpenTelemetryResourceAutoConfiguration.class},
    before = {CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class}
)
@ConditionalOnProperty(prefix = MicrometerRegistryOtlpProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnOpenTelemetryMetricsExporter("otlp")
@EnableConfigurationProperties(MicrometerRegistryOtlpProperties.class)
public final class MicrometerRegistryOtlpAutoConfiguration {

    /**
     * The {@link OtlpMeterRegistry} logs warnings if any of these attributes are provided.
     * So, we ensure to remove them when configuring the registry.
     */
    private static final Set<String> RESERVED_RESOURCE_ATTRIBUTES = new HashSet<>(
            Arrays.asList("telemetry.sdk.language", "telemetry.sdk.name", "telemetry.sdk.version"));

    @Bean
    @ConditionalOnMissingBean
    MicrometerOtlpConfig otlpConfig(OtlpMetricsConnectionDetails connectionDetails,
                                    OpenTelemetryExporterProperties commonProperties,
                                    OpenTelemetryMetricsExporterProperties metricsProperties,
                                    MicrometerRegistryOtlpProperties registryProperties,
                                    Resource resource
    ) {
        Protocol protocol = metricsProperties.getOtlp().getProtocol() != null ? metricsProperties.getOtlp().getProtocol() : commonProperties.getOtlp().getProtocol();
        return MicrometerOtlpConfig.builder()
                .url(connectionDetails.getUrl(protocol))
                .step(metricsProperties.getInterval())
                .aggregationTemporality(switch(metricsProperties.getAggregationTemporality()) {
                    case DELTA -> AggregationTemporality.DELTA;
                    case CUMULATIVE, LOW_MEMORY -> AggregationTemporality.CUMULATIVE;
                })
                .histogramFlavor(switch (metricsProperties.getHistogramAggregation()) {
                    case EXPLICIT_BUCKET_HISTOGRAM -> HistogramFlavor.EXPLICIT_BUCKET_HISTOGRAM;
                    case BASE2_EXPONENTIAL_BUCKET_HISTOGRAM -> HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM;
                })
                .addHeaders(commonProperties.getOtlp().getHeaders())
                .addHeaders(metricsProperties.getOtlp().getHeaders())
                .addResourceAttributes(resource.getAttributes().asMap().entrySet().stream()
                        .filter(entry -> !RESERVED_RESOURCE_ATTRIBUTES.contains(entry.getKey().getKey()))
                        .collect(HashMap::new, (m, e) -> m.put(e.getKey().getKey(), e.getValue().toString()), HashMap::putAll))
                .maxScale(registryProperties.getMaxScale())
                .maxBucketCount(registryProperties.getMaxBucketCount())
                .baseTimeUnit(registryProperties.getBaseTimeUnit())
                .build();
    }

    @Bean
    @ConditionalOnThreading(Threading.PLATFORM)
    OtlpMeterRegistry otlpMeterRegistryPlatformThreads(Clock clock, OtlpConfig otlpConfig, ObjectProvider<OtlpMetricsSender> otlpMetricsSender) {
        var builder = OtlpMeterRegistry.builder(otlpConfig).clock(clock);
        otlpMetricsSender.ifAvailable(builder::metricsSender);
        return builder.build();
    }

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    OtlpMeterRegistry otlpMeterRegistryVirtualThreads(Clock clock, OtlpConfig otlpConfig, ObjectProvider<OtlpMetricsSender> otlpMetricsSender) {
        VirtualThreadTaskExecutor executor = new VirtualThreadTaskExecutor("otlp-meter-registry-");
        var builder = OtlpMeterRegistry.builder(otlpConfig)
                .clock(clock)
                .threadFactory(executor.getVirtualThreadFactory());
        otlpMetricsSender.ifAvailable(builder::metricsSender);
        return builder.build();
    }

}
