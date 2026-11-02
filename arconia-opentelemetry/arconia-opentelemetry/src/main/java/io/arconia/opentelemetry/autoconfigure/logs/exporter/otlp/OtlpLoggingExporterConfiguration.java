package io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;

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

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.ConditionalOnOtlpProtocol;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.EffectiveOtlpExporterConfig;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.ConditionalOnOpenTelemetryLoggingExporter;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;

/**
 * Configuration for exporting logs via OTLP.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OtlpHttpLogRecordExporter.class)
@ConditionalOnOpenTelemetryLoggingExporter("otlp")
public final class OtlpLoggingExporterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OtlpLoggingExporterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    OtlpLoggingConnectionDetails otlpLoggingConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryLoggingExporterProperties properties, ObjectProvider<SslBundles> sslBundles) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        return new OtlpLoggingConnectionDetails() {

            @Override
            public String getLogsUrl(Protocol protocol) {
                return config.getUrl(protocol, LOGS_PATH);
            }

            @Override
            public @Nullable SslBundle getSslBundle() {
                return config.getSslBundle(sslBundles.getIfAvailable());
            }

        };
    }

    // TODO: Add proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpLoggingConnectionDetails.class)
    @ConditionalOnOtlpProtocol(prefix = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX, value = Protocol.HTTP_PROTOBUF)
    OtlpHttpLogRecordExporter otlpHttpLogRecordExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryLoggingExporterProperties properties, OtlpLoggingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        String url = connectionDetails.getLogsUrl(Protocol.HTTP_PROTOBUF);
        OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
                .setEndpoint(url)
                .setTimeout(config.getTimeout())
                .setConnectTimeout(config.getConnectTimeout())
                .setCompression(config.getCompression())
                .setRetryPolicy(config.getRetryPolicy())
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
        logger.info("Configuring OpenTelemetry HTTP/Protobuf log exporter with endpoint: {}", EffectiveOtlpExporterConfig.toLoggableUrl(url));
        return builder.build();
    }

    // TODO: Add proxy.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpLoggingConnectionDetails.class)
    @ConditionalOnOtlpProtocol(prefix = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX, value = Protocol.GRPC)
    OtlpGrpcLogRecordExporter otlpGrpcLogRecordExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryLoggingExporterProperties properties, OtlpLoggingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        String url = connectionDetails.getLogsUrl(Protocol.GRPC);
        OtlpGrpcLogRecordExporterBuilder builder = OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(url)
                .setTimeout(config.getTimeout())
                .setConnectTimeout(config.getConnectTimeout())
                .setCompression(config.getCompression())
                .setRetryPolicy(config.getRetryPolicy())
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
        logger.info("Configuring OpenTelemetry gRPC log exporter with endpoint: {}", EffectiveOtlpExporterConfig.toLoggableUrl(url));
        return builder.build();
    }

}
