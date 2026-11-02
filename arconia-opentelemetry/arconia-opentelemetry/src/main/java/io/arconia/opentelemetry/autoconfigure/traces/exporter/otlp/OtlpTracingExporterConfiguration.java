package io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp;

import java.util.concurrent.Executors;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.common.export.ProxyOptions;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.util.Assert;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.ConditionalOnOtlpProtocol;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.EffectiveOtlpExporterConfig;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.ConditionalOnOpenTelemetryTracingExporter;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterProperties;

/**
 * Auto-configuration for exporting traces via OTLP.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OtlpHttpSpanExporter.class)
@ConditionalOnOpenTelemetryTracingExporter("otlp")
public final class OtlpTracingExporterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OtlpTracingExporterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    OtlpTracingConnectionDetails otlpTracingConnectionDetails(OpenTelemetryExporterProperties commonProperties, OpenTelemetryTracingExporterProperties properties, ObjectProvider<SslBundles> sslBundles) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        return new OtlpTracingConnectionDetails() {

            @Override
            public String getTracesUrl(Protocol protocol) {
                return config.getUrl(protocol, TRACES_PATH);
            }

            @Override
            public @Nullable SslBundle getSslBundle() {
                return config.getSslBundle(sslBundles.getIfAvailable());
            }

        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpTracingConnectionDetails.class)
    @ConditionalOnOtlpProtocol(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX, value = Protocol.HTTP_PROTOBUF)
    OtlpHttpSpanExporter otlpHttpSpanExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryTracingExporterProperties properties, OtlpTracingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider, Environment environment) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        String url = connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF);
        OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
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
        ProxyOptions proxyOptions = config.getProxyOptions();
        if (proxyOptions != null) {
            builder.setProxy(proxyOptions);
        }
        if (config.isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
            builder.setInternalTelemetryVersion(InternalTelemetryVersion.LATEST);
        }
        if (Threading.VIRTUAL.isActive(environment)) {
            builder.setExecutorService(Executors.newThreadPerTaskExecutor(
                    new VirtualThreadTaskExecutor("otel-traces-exporter-").getVirtualThreadFactory()));
        }
        logger.info("Configuring OpenTelemetry HTTP/Protobuf span exporter with endpoint: {}", EffectiveOtlpExporterConfig.toLoggableUrl(url));
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OtlpTracingConnectionDetails.class)
    @ConditionalOnOtlpProtocol(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX, value = Protocol.GRPC)
    OtlpGrpcSpanExporter otlpGrpcSpanExporter(OpenTelemetryExporterProperties commonProperties, OpenTelemetryTracingExporterProperties properties, OtlpTracingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
        EffectiveOtlpExporterConfig config = new EffectiveOtlpExporterConfig(commonProperties, properties.getOtlp());
        Assert.state(!config.hasProxy(), "The OTLP exporter doesn't support a proxy with the gRPC protocol. Use the HTTP/Protobuf protocol, or remove the proxy configuration.");
        String url = connectionDetails.getTracesUrl(Protocol.GRPC);
        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
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
        logger.info("Configuring OpenTelemetry gRPC span exporter with endpoint: {}", EffectiveOtlpExporterConfig.toLoggableUrl(url));
        return builder.build();
    }

}
