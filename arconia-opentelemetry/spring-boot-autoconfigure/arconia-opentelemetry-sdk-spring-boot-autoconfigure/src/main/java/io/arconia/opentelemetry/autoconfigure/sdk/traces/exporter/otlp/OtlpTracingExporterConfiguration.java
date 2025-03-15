package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.otlp;

import java.util.Locale;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;

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
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.OpenTelemetryTracingExporterProperties;

/**
 * Auto-configuration for exporting traces via OTLP.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OtlpHttpSpanExporter.class)
@ConditionalOnProperty(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "otlp", matchIfMissing = true)
public class OtlpTracingExporterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OtlpTracingExporterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(OtlpTracingConnectionDetails.class)
    PropertiesOtlpTracingConnectionDetails otlpTracingConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryTracingExporterProperties properties) {
        return new PropertiesOtlpTracingConnectionDetails(commonProperties, properties);
    }

    // TODO: Add certificates/TLS, retry, and proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpTracingConnectionDetails.class)
    @ConditionalOnProperty(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp", name = "protocol", havingValue = "http_protobuf", matchIfMissing = true)
    OtlpHttpSpanExporter otlpHttpSpanExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryTracingExporterProperties properties, OtlpTracingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
                .setEndpoint(connectionDetails.getUrl(Protocol.HTTP_PROTOBUF))
                .setTimeout(properties.getOtlp().getTimeout() != null ? properties.getOtlp().getTimeout() : commonProperties.getOtlp().getTimeout())
                .setConnectTimeout(properties.getOtlp().getConnectTimeout() != null ? properties.getOtlp().getConnectTimeout() : commonProperties.getOtlp().getConnectTimeout())
                .setCompression(properties.getOtlp().getCompression() != null ? properties.getOtlp().getCompression().name().toLowerCase(Locale.ROOT) : commonProperties.getOtlp().getCompression().name().toLowerCase(Locale.ROOT))
                .setMemoryMode(commonProperties.getMemoryMode());
        commonProperties.getOtlp().getHeaders().forEach(builder::addHeader);
        properties.getOtlp().getHeaders().forEach(builder::addHeader);
        if (properties.getOtlp().isMetrics() != null && Boolean.TRUE.equals(properties.getOtlp().isMetrics())
                || properties.getOtlp().isMetrics() == null && commonProperties.getOtlp().isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
        }
        logger.info("Configuring OpenTelemetry HTTP/Protobuf span exporter with endpoint: {}", connectionDetails.getUrl(Protocol.HTTP_PROTOBUF));
        return builder.build();
    }

    // TODO: Add certificates/TLS, retry, and proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpTracingConnectionDetails.class)
    @ConditionalOnProperty(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp", name = "protocol", havingValue = "grpc")
    OtlpGrpcSpanExporter otlpGrpcSpanExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryTracingExporterProperties properties, OtlpTracingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(connectionDetails.getUrl(Protocol.GRPC))
                .setTimeout(properties.getOtlp().getTimeout() != null ? properties.getOtlp().getTimeout() : commonProperties.getOtlp().getTimeout())
                .setConnectTimeout(properties.getOtlp().getConnectTimeout() != null ? properties.getOtlp().getConnectTimeout() : commonProperties.getOtlp().getConnectTimeout())
                .setCompression(properties.getOtlp().getCompression() != null ? properties.getOtlp().getCompression().name().toLowerCase(Locale.ROOT) : commonProperties.getOtlp().getCompression().name().toLowerCase(Locale.ROOT))
                .setMemoryMode(commonProperties.getMemoryMode());
        commonProperties.getOtlp().getHeaders().forEach(builder::addHeader);
        properties.getOtlp().getHeaders().forEach(builder::addHeader);
        if (properties.getOtlp().isMetrics() != null && Boolean.TRUE.equals(properties.getOtlp().isMetrics())
                || properties.getOtlp().isMetrics() == null && commonProperties.getOtlp().isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
        }
        logger.info("Configuring OpenTelemetry gRPC span exporter with endpoint: {}", connectionDetails.getUrl(Protocol.GRPC));
        return builder.build();
    }

    /**
     * Implementation of {@link OtlpTracingConnectionDetails} that uses properties to determine the OTLP endpoint.
     */
    static class PropertiesOtlpTracingConnectionDetails implements OtlpTracingConnectionDetails {

        private final OpenTelemetryExporterProperties commonProperties;
        private final OpenTelemetryTracingExporterProperties properties;

        public PropertiesOtlpTracingConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryTracingExporterProperties properties) {
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
                url = protocolProperty == Protocol.HTTP_PROTOBUF ? commonProperties.getOtlp().getEndpoint().resolve(TRACES_PATH).toString() : commonProperties.getOtlp().getEndpoint().toString();
            } else {
                url = protocolProperty == Protocol.HTTP_PROTOBUF ? DEFAULT_HTTP_PROTOBUF_ENDPOINT : DEFAULT_GRPC_ENDPOINT;
            }
            return url;
        }

    }

}
