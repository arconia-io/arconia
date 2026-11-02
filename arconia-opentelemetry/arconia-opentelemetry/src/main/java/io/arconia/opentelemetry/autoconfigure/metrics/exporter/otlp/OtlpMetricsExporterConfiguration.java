package io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.ConditionalOnOtlpProtocol;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.EffectiveOtlpExporterConfig;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
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
    OtlpMetricsConnectionDetails otlpMetricsConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties, ObjectProvider<SslBundles> sslBundles) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        return new OtlpMetricsConnectionDetails() {

            @Override
            public String getMetricsUrl(Protocol protocol) {
                return config.getUrl(protocol, METRICS_PATH);
            }

            @Override
            public @Nullable SslBundle getSslBundle() {
                return config.getSslBundle(sslBundles.getIfAvailable());
            }

        };
    }

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
                .setDefaultAggregationSelector(getDefaultAggregationSelector(properties))
                .setMemoryMode(config.getMemoryMode());
        config.getHeaders(connectionDetails).forEach(builder::addHeader);
        SslBundle sslBundle = connectionDetails.getSslBundle();
        if (sslBundle != null) {
            builder.setSslContext(sslBundle.createSslContext(), EffectiveOtlpExporterConfig.getTrustManager(sslBundle));
        }
        ProxyOptions proxyOptions = config.getProxyOptions();
        if (proxyOptions != null) {
            builder.setProxyOptions(proxyOptions);
        }
        if (config.isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
            builder.setInternalTelemetryVersion(InternalTelemetryVersion.LATEST);
        }
        logger.info("Configuring OpenTelemetry HTTP/Protobuf metric exporter with endpoint: {}", EffectiveOtlpExporterConfig.toLoggableUrl(url));
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpMetricsConnectionDetails.class)
    @ConditionalOnOtlpProtocol(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX, value = Protocol.GRPC)
    OtlpGrpcMetricExporter otlpGrpcMetricExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties, OtlpMetricsConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        Assert.state(!config.hasProxy(), "The OTLP exporter doesn't support a proxy with the gRPC protocol. Use the HTTP/Protobuf protocol, or remove the proxy configuration.");
        String url = connectionDetails.getMetricsUrl(Protocol.GRPC);
        OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder()
                .setEndpoint(url)
                .setTimeout(config.getTimeout())
                .setConnectTimeout(config.getConnectTimeout())
                .setCompression(config.getCompression())
                .setRetryPolicy(config.getRetryPolicy())
                .setAggregationTemporalitySelector(getAggregationTemporalitySelector(properties))
                .setDefaultAggregationSelector(getDefaultAggregationSelector(properties))
                .setMemoryMode(config.getMemoryMode());
        config.getHeaders(connectionDetails).forEach(builder::addHeader);
        SslBundle sslBundle = connectionDetails.getSslBundle();
        if (sslBundle != null) {
            builder.setSslContext(sslBundle.createSslContext(), EffectiveOtlpExporterConfig.getTrustManager(sslBundle));
        }
        if (config.isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
            builder.setInternalTelemetryVersion(InternalTelemetryVersion.LATEST);
        }
        logger.info("Configuring OpenTelemetry gRPC metric exporter with endpoint: {}", EffectiveOtlpExporterConfig.toLoggableUrl(url));
        return builder.build();
    }

    AggregationTemporalitySelector getAggregationTemporalitySelector(OpenTelemetryMetricsExporterProperties properties) {
        return switch (properties.getAggregationTemporality()) {
            case CUMULATIVE -> AggregationTemporalitySelector.alwaysCumulative();
            case DELTA -> AggregationTemporalitySelector.deltaPreferred();
            case LOW_MEMORY -> AggregationTemporalitySelector.lowMemory();
        };
    }

    DefaultAggregationSelector getDefaultAggregationSelector(OpenTelemetryMetricsExporterProperties properties) {
        return switch (properties.getHistogramAggregation()) {
            case EXPLICIT_BUCKET_HISTOGRAM -> DefaultAggregationSelector.getDefault();
            case BASE2_EXPONENTIAL_BUCKET_HISTOGRAM -> DefaultAggregationSelector.getDefault()
                    .with(InstrumentType.HISTOGRAM, Aggregation.base2ExponentialBucketHistogram());
        };
    }

}
