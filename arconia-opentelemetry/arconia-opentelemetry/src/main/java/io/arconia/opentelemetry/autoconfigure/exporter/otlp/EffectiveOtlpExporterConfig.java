package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.common.export.RetryPolicy;

import org.springframework.util.Assert;

import io.arconia.core.annotations.Internal;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;

/**
 * The effective OTLP exporter configuration of a signal, where each signal-specific property
 * takes precedence over the common one.
 */
@Internal
public final class EffectiveOtlpExporterConfig {

    private final OpenTelemetryExporterProperties commonProperties;

    private final OtlpExporterConfig signalProperties;

    public EffectiveOtlpExporterConfig(OpenTelemetryExporterProperties commonProperties, OtlpExporterConfig signalProperties) {
        Assert.notNull(commonProperties, "commonProperties cannot be null");
        Assert.notNull(signalProperties, "signalProperties cannot be null");
        this.commonProperties = commonProperties;
        this.signalProperties = signalProperties;
    }

    public Protocol getProtocol() {
        Protocol protocol = signalProperties.getProtocol();
        return protocol != null ? protocol : commonProperties.getOtlp().getProtocol();
    }

    public String getUrl(Protocol protocol, String signalPath) {
        Assert.notNull(protocol, "protocol cannot be null");
        Assert.hasText(signalPath, "signalPath cannot be null or empty");

        URI signalEndpoint = signalProperties.getEndpoint();
        if (signalEndpoint != null) {
            return signalEndpoint.toString();
        }

        URI commonEndpoint = commonProperties.getOtlp().getEndpoint();
        if (commonEndpoint != null) {
            if (protocol == Protocol.GRPC) {
                return commonEndpoint.toString();
            }
            return commonEndpoint + (commonEndpoint.getPath().endsWith("/") ? signalPath.substring(1) : signalPath);
        }

        return switch (protocol) {
            case HTTP_PROTOBUF -> "http://localhost:" + OtlpConnectionDetails.DEFAULT_HTTP_PORT + signalPath;
            case GRPC -> "http://localhost:" + OtlpConnectionDetails.DEFAULT_GRPC_PORT;
        };
    }

    public Duration getTimeout() {
        Duration timeout = signalProperties.getTimeout();
        return timeout != null ? timeout : commonProperties.getOtlp().getTimeout();
    }

    public Duration getConnectTimeout() {
        Duration connectTimeout = signalProperties.getConnectTimeout();
        return connectTimeout != null ? connectTimeout : commonProperties.getOtlp().getConnectTimeout();
    }

    public String getCompression() {
        Compression compression = signalProperties.getCompression();
        return (compression != null ? compression : commonProperties.getOtlp().getCompression()).name().toLowerCase(Locale.ROOT);
    }

    public RetryPolicy getRetryPolicy() {
        RetryConfig retry = signalProperties.getRetry();
        return RetryConfig.buildRetryPolicy(retry != null ? retry : commonProperties.getOtlp().getRetry());
    }

    public MemoryMode getMemoryMode() {
        return commonProperties.getMemoryMode();
    }

    public Map<String, String> getHeaders(OtlpConnectionDetails connectionDetails) {
        Assert.notNull(connectionDetails, "connectionDetails cannot be null");
        Map<String, String> headers = new LinkedHashMap<>(connectionDetails.getHeaders());
        headers.putAll(commonProperties.getOtlp().getHeaders());
        headers.putAll(signalProperties.getHeaders());
        return headers;
    }

    public boolean isMetrics() {
        Boolean metrics = signalProperties.isMetrics();
        return metrics != null ? metrics : commonProperties.getOtlp().isMetrics();
    }

}
