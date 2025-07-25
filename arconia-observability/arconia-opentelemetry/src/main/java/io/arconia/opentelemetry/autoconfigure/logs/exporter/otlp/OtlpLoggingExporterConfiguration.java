package io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp;

import java.util.Locale;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;

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

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.ConditionalOnOpenTelemetryLoggingExporter;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;

/**
 * Configuration for exporting logs via OTLP.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OtlpHttpLogRecordExporter.class)
@ConditionalOnOpenTelemetryLoggingExporter("otlp")
public class OtlpLoggingExporterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OtlpLoggingExporterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    OtlpLoggingConnectionDetails otlpLoggingConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryLoggingExporterProperties properties) {
        return new PropertiesOtlpLoggingConnectionDetails(commonProperties, properties);
    }

    // TODO: Add certificates/TLS, retry, and proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpLoggingConnectionDetails.class)
    @ConditionalOnProperty(prefix = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp", name = "protocol", havingValue = "http_protobuf", matchIfMissing = true)
    OtlpHttpLogRecordExporter otlpHttpLogRecordExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryLoggingExporterProperties properties, OtlpLoggingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
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
        logger.info("Configuring OpenTelemetry HTTP/Protobuf log exporter with endpoint: {}", connectionDetails.getUrl(Protocol.HTTP_PROTOBUF));
        return builder.build();
    }

    // TODO: Add certificates/TLS, retry, and proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpLoggingConnectionDetails.class)
    @ConditionalOnProperty(prefix = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp", name = "protocol", havingValue = "grpc")
    OtlpGrpcLogRecordExporter otlpGrpcLogRecordExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryLoggingExporterProperties properties, OtlpLoggingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        OtlpGrpcLogRecordExporterBuilder builder = OtlpGrpcLogRecordExporter.builder()
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
        logger.info("Configuring OpenTelemetry gRPC log exporter with endpoint: {}", connectionDetails.getUrl(Protocol.GRPC));
        return builder.build();
    }

    /**
     * Implementation of {@link OtlpLoggingConnectionDetails} that uses properties to determine the OTLP endpoint.
     */
    static class PropertiesOtlpLoggingConnectionDetails implements OtlpLoggingConnectionDetails {

        private final OpenTelemetryExporterProperties commonProperties;
        private final OpenTelemetryLoggingExporterProperties properties;

        public PropertiesOtlpLoggingConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryLoggingExporterProperties properties) {
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
                url = protocolProperty == Protocol.HTTP_PROTOBUF ? commonProperties.getOtlp().getEndpoint().resolve(LOGS_PATH).toString() : commonProperties.getOtlp().getEndpoint().toString();
            } else {
                url = protocolProperty == Protocol.HTTP_PROTOBUF ? DEFAULT_HTTP_PROTOBUF_ENDPOINT : DEFAULT_GRPC_ENDPOINT;
            }
            return url;
        }

    }

}
