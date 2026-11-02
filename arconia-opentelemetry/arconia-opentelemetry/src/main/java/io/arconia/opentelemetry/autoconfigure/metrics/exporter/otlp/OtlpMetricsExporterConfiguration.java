package io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.internal.view.Base2ExponentialHistogramAggregation;
import io.opentelemetry.sdk.metrics.internal.view.ExplicitBucketHistogramAggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.ConditionalOnOtlpProtocol;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.EffectiveOtlpExporterConfig;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMeterProviderBuilderCustomizer;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.ConditionalOnOpenTelemetryMetricsExporter;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;

/**
 * Auto-configuration for exporting metrics via OTLP.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OtlpHttpMetricExporter.class)
@ConditionalOnOpenTelemetryMetricsExporter("otlp")
public final class OtlpMetricsExporterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OtlpMetricsExporterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    OtlpMetricsConnectionDetails otlpMetricsConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        return protocol -> config.getUrl(protocol, OtlpMetricsConnectionDetails.METRICS_PATH);
    }

    // TODO: Add certificates/TLS and proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpMetricsConnectionDetails.class)
    @ConditionalOnOtlpProtocol(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX, value = Protocol.HTTP_PROTOBUF)
    OtlpHttpMetricExporter otlpHttpMetricExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties, OtlpMetricsConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        String url = connectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF);
        OtlpHttpMetricExporterBuilder builder = OtlpHttpMetricExporter.builder()
                .setEndpoint(url)
                .setTimeout(config.getTimeout())
                .setConnectTimeout(config.getConnectTimeout())
                .setCompression(config.getCompression())
                .setRetryPolicy(config.getRetryPolicy())
                .setAggregationTemporalitySelector(getAggregationTemporalitySelector(properties))
                .setMemoryMode(config.getMemoryMode());
        config.getHeaders(connectionDetails).forEach(builder::addHeader);
        if (config.isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
            builder.setInternalTelemetryVersion(InternalTelemetryVersion.LATEST);
        }
        logger.info("Configuring OpenTelemetry HTTP/Protobuf metric exporter with endpoint: {}", url);
        return builder.build();
    }

    // TODO: Add certificates/TLS and proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpMetricsConnectionDetails.class)
    @ConditionalOnOtlpProtocol(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX, value = Protocol.GRPC)
    OtlpGrpcMetricExporter otlpGrpcMetricExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties, OtlpMetricsConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        String url = connectionDetails.getMetricsUrl(Protocol.GRPC);
        OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder()
                .setEndpoint(url)
                .setTimeout(config.getTimeout())
                .setConnectTimeout(config.getConnectTimeout())
                .setCompression(config.getCompression())
                .setRetryPolicy(config.getRetryPolicy())
                .setAggregationTemporalitySelector(getAggregationTemporalitySelector(properties))
                .setMemoryMode(config.getMemoryMode());
        config.getHeaders(connectionDetails).forEach(builder::addHeader);
        if (config.isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
            builder.setInternalTelemetryVersion(InternalTelemetryVersion.LATEST);
        }
        logger.info("Configuring OpenTelemetry gRPC metric exporter with endpoint: {}", url);
        return builder.build();
    }

    AggregationTemporalitySelector getAggregationTemporalitySelector(OpenTelemetryMetricsExporterProperties properties) {
        return switch (properties.getAggregationTemporality()) {
            case CUMULATIVE -> AggregationTemporalitySelector.alwaysCumulative();
            case DELTA -> AggregationTemporalitySelector.deltaPreferred();
            case LOW_MEMORY -> AggregationTemporalitySelector.lowMemory();
        };
    }

    @Bean
    OpenTelemetryMeterProviderBuilderCustomizer histogramAggregation(OpenTelemetryMetricsExporterProperties properties) {
        return builder -> builder.registerView(
                InstrumentSelector.builder()
                        .setType(InstrumentType.HISTOGRAM)
                        .build(),
                View.builder()
                        .setAggregation(switch(properties.getHistogramAggregation()) {
                            case BASE2_EXPONENTIAL_BUCKET_HISTOGRAM -> Base2ExponentialHistogramAggregation.getDefault();
                            case EXPLICIT_BUCKET_HISTOGRAM -> ExplicitBucketHistogramAggregation.getDefault();
                        })
                        .build());
    }

}
