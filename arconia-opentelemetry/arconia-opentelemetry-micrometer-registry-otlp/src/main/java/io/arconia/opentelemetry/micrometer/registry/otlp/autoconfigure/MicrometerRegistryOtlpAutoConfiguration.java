package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.CompressionMode;
import io.micrometer.registry.otlp.HistogramFlavor;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpHttpMetricsSender;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.util.Assert;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.EffectiveOtlpExporterConfig;
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
    after = {MetricsAutoConfiguration.class, OpenTelemetryExporterAutoConfiguration.class, OpenTelemetryMetricsAutoConfiguration.class, OpenTelemetryMetricsExporterAutoConfiguration.class, OpenTelemetryResourceAutoConfiguration.class},
    before = {OtlpMetricsExportAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class}
)
// Mutually exclusive with the Micrometer Metrics OpenTelemetry Bridge.
@ConditionalOnProperty(prefix = "arconia.otel.metrics.micrometer-bridge", name = "enabled", havingValue = "false", matchIfMissing = true)
@ConditionalOnProperty(prefix = MicrometerRegistryOtlpProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnOpenTelemetryMetricsExporter("otlp")
@EnableConfigurationProperties(MicrometerRegistryOtlpProperties.class)
public final class MicrometerRegistryOtlpAutoConfiguration {

    /**
     * The {@link OtlpMeterRegistry} logs warnings if any of these attributes are provided.
     * So, we ensure to remove them when configuring the registry.
     */
    private static final Set<String> RESERVED_RESOURCE_ATTRIBUTES = Set.of("telemetry.sdk.language", "telemetry.sdk.name", "telemetry.sdk.version");

    @Bean
    @ConditionalOnMissingBean(OtlpConfig.class)
    MicrometerOtlpConfig otlpConfig(OtlpMetricsConnectionDetails connectionDetails,
                                    OpenTelemetryExporterProperties commonProperties,
                                    OpenTelemetryMetricsExporterProperties metricsProperties,
                                    MicrometerRegistryOtlpProperties registryProperties,
                                    Resource resource
    ) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, metricsProperties.getOtlp());
        Assert.state(config.getProtocol() == Protocol.HTTP_PROTOBUF, "The Micrometer OTLP registry only supports the HTTP/Protobuf protocol. "
                + "Set 'arconia.otel.metrics.exporter.otlp.protocol' to 'http-protobuf', or use the Micrometer metrics bridge to export metrics via gRPC.");
        return MicrometerOtlpConfig.builder()
                .url(connectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF))
                .step(metricsProperties.getInterval())
                .aggregationTemporality(switch(metricsProperties.getAggregationTemporality()) {
                    case DELTA -> AggregationTemporality.DELTA;
                    case CUMULATIVE, LOW_MEMORY -> AggregationTemporality.CUMULATIVE;
                })
                .histogramFlavor(switch (metricsProperties.getHistogramAggregation()) {
                    case EXPLICIT_BUCKET_HISTOGRAM -> HistogramFlavor.EXPLICIT_BUCKET_HISTOGRAM;
                    case BASE2_EXPONENTIAL_BUCKET_HISTOGRAM -> HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM;
                })
                .addHeaders(config.getHeaders(connectionDetails))
                .addResourceAttributes(resource.getAttributes().asMap().entrySet().stream()
                        .filter(entry -> !RESERVED_RESOURCE_ATTRIBUTES.contains(entry.getKey().getKey()))
                        .collect(HashMap::new, (m, e) -> m.put(e.getKey().getKey(), e.getValue().toString()), HashMap::putAll))
                .maxScale(registryProperties.getMaxScale())
                .maxBucketCount(registryProperties.getMaxBucketCount())
                .baseTimeUnit(registryProperties.getBaseTimeUnit())
                .compressionMode(CompressionMode.valueOf(config.getCompression().toUpperCase(Locale.ROOT)))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(OtlpMetricsSender.class)
    OtlpHttpMetricsSender otlpMetricsSender(OtlpMetricsConnectionDetails connectionDetails,
                                            OpenTelemetryExporterProperties commonProperties,
                                            OpenTelemetryMetricsExporterProperties metricsProperties
    ) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, metricsProperties.getOtlp());
        ProxyOptions proxyOptions = config.getProxyOptions();
        return new OtlpHttpMetricsSender(new JdkHttpClientSender(config.getConnectTimeout(), config.getTimeout(),
                connectionDetails.getSslBundle(), proxyOptions != null ? proxyOptions.getProxySelector() : null));
    }

    @Bean
    @ConditionalOnMissingBean
    OtlpMeterRegistry otlpMeterRegistry(Clock clock, OtlpConfig otlpConfig, OtlpMetricsSender otlpMetricsSender, Environment environment) {
        OtlpMeterRegistry.Builder builder = OtlpMeterRegistry.builder(otlpConfig)
                .clock(clock)
                .metricsSender(otlpMetricsSender);
        if (Threading.VIRTUAL.isActive(environment)) {
            builder.threadFactory(new VirtualThreadTaskExecutor("otlp-meter-registry-").getVirtualThreadFactory());
        }
        return builder.build();
    }

}
