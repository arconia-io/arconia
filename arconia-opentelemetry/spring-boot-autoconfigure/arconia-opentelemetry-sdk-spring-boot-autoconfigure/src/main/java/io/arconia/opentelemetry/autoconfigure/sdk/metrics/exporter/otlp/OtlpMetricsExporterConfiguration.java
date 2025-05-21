package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.otlp;

import java.util.Locale;

import io.arconia.opentelemetry.autoconfigure.sdk.metrics.OpenTelemetryMeterProviderBuilderCustomizer;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.OpenTelemetryMetricsExporterProperties;

/**
 * Auto-configuration for exporting metrics via OTLP.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OtlpHttpMetricExporter.class)
@ConditionalOnProperty(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "otlp", matchIfMissing = true)
public class OtlpMetricsExporterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OtlpMetricsExporterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(OtlpMetricsConnectionDetails.class)
    PropertiesOtlpMetricsConnectionDetails otlpMetricsConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties) {
        return new PropertiesOtlpMetricsConnectionDetails(commonProperties, properties);
    }

    // TODO: Add certificates/TLS, retry, and proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpMetricsConnectionDetails.class)
    @ConditionalOnProperty(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp", name = "protocol", havingValue = "http_protobuf", matchIfMissing = true)
    OtlpHttpMetricExporter otlpHttpMetricExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties, OtlpMetricsConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        OtlpHttpMetricExporterBuilder builder = OtlpHttpMetricExporter.builder()
                .setEndpoint(connectionDetails.getUrl(Protocol.HTTP_PROTOBUF))
                .setTimeout(properties.getOtlp().getTimeout() != null ? properties.getOtlp().getTimeout() : commonProperties.getOtlp().getTimeout())
                .setConnectTimeout(properties.getOtlp().getConnectTimeout() != null ? properties.getOtlp().getConnectTimeout() : commonProperties.getOtlp().getConnectTimeout())
                .setCompression(properties.getOtlp().getCompression() != null ? properties.getOtlp().getCompression().name().toLowerCase(Locale.ROOT) : commonProperties.getOtlp().getCompression().name().toLowerCase(Locale.ROOT))
                .setAggregationTemporalitySelector(getAggregationTemporalitySelector(properties))
                .setMemoryMode(commonProperties.getMemoryMode());
        commonProperties.getOtlp().getHeaders().forEach(builder::addHeader);
        properties.getOtlp().getHeaders().forEach(builder::addHeader);
        if (properties.getOtlp().isMetrics() != null && Boolean.TRUE.equals(properties.getOtlp().isMetrics())
                || properties.getOtlp().isMetrics() == null && commonProperties.getOtlp().isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
        }
        logger.info("Configuring OpenTelemetry HTTP/Protobuf metric exporter with endpoint: {}", connectionDetails.getUrl(Protocol.HTTP_PROTOBUF));
        return builder.build();
    }

    // TODO: Add certificates/TLS, retry, and proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpMetricsConnectionDetails.class)
    @ConditionalOnProperty(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp", name = "protocol", havingValue = "grpc")
    OtlpGrpcMetricExporter otlpGrpcMetricExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties, OtlpMetricsConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder()
                .setEndpoint(connectionDetails.getUrl(Protocol.GRPC))
                .setTimeout(properties.getOtlp().getTimeout() != null ? properties.getOtlp().getTimeout() : commonProperties.getOtlp().getTimeout())
                .setConnectTimeout(properties.getOtlp().getConnectTimeout() != null ? properties.getOtlp().getConnectTimeout() : commonProperties.getOtlp().getConnectTimeout())
                .setCompression(properties.getOtlp().getCompression() != null ? properties.getOtlp().getCompression().name().toLowerCase(Locale.ROOT) : commonProperties.getOtlp().getCompression().name().toLowerCase(Locale.ROOT))
                .setAggregationTemporalitySelector(getAggregationTemporalitySelector(properties))
                .setMemoryMode(commonProperties.getMemoryMode());
        commonProperties.getOtlp().getHeaders().forEach(builder::addHeader);
        properties.getOtlp().getHeaders().forEach(builder::addHeader);
        if (properties.getOtlp().isMetrics() != null && Boolean.TRUE.equals(properties.getOtlp().isMetrics())
                || properties.getOtlp().isMetrics() == null && commonProperties.getOtlp().isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
        }
        logger.info("Configuring OpenTelemetry gRPC metric exporter with endpoint: {}", connectionDetails.getUrl(Protocol.GRPC));
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

    /**
     * Implementation of {@link OtlpMetricsConnectionDetails} that uses properties to determine the OTLP endpoint.
     */
    static class PropertiesOtlpMetricsConnectionDetails implements OtlpMetricsConnectionDetails {

        private final OpenTelemetryExporterProperties commonProperties;
        private final OpenTelemetryMetricsExporterProperties properties;

        public PropertiesOtlpMetricsConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryMetricsExporterProperties properties) {
            this.commonProperties = commonProperties;
            this.properties = properties;
        }

        @Override
        public String getUrl(Protocol protocol) {
            var protocolProperty = properties.getOtlp().getProtocol() != null ? properties.getOtlp().getProtocol() : commonProperties.getOtlp().getProtocol();
            Assert.state(protocol == protocolProperty, "Requested protocol %s doesn't match configured protocol %s".formatted(protocol, protocolProperty));

            String url;
            if (properties.getOtlp().getEndpoint() != null) {
                url = properties.getOtlp().getEndpoint().toString();
            } else if (commonProperties.getOtlp().getEndpoint() != null) {
                url = protocolProperty == Protocol.HTTP_PROTOBUF ? commonProperties.getOtlp().getEndpoint().resolve(METRICS_PATH).toString() : commonProperties.getOtlp().getEndpoint().toString();
            } else {
                url = protocolProperty == Protocol.HTTP_PROTOBUF ? DEFAULT_HTTP_PROTOBUF_ENDPOINT : DEFAULT_GRPC_ENDPOINT;
            }
            return url;
        }

    }

}
