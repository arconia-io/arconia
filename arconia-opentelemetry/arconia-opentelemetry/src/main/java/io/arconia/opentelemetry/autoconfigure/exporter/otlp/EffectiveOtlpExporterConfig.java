package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.common.export.RetryPolicy;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
        SignalRetryConfig retry = signalProperties.getRetry();
        RetryConfig commonRetry = commonProperties.getOtlp().getRetry();
        Integer maxAttempts = retry.getMaxAttempts();
        Duration firstBackoff = retry.getBackoffConfig().getFirstBackoff();
        Duration maxBackoff = retry.getBackoffConfig().getMaxBackoff();
        Double multiplier = retry.getBackoffConfig().getMultiplier();
        return RetryPolicy.builder()
                .setMaxAttempts(maxAttempts != null ? maxAttempts : commonRetry.getMaxAttempts())
                .setInitialBackoff(firstBackoff != null ? firstBackoff : commonRetry.getBackoffConfig().getFirstBackoff())
                .setMaxBackoff(maxBackoff != null ? maxBackoff : commonRetry.getBackoffConfig().getMaxBackoff())
                .setBackoffMultiplier(multiplier != null ? multiplier : commonRetry.getBackoffConfig().getMultiplier())
                .build();
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

    /**
     * Get the name of the SSL bundle to use for the connection to the endpoint, if any.
     */
    @Nullable
    public String getSslBundleName() {
        String bundleName = signalProperties.getSsl().getBundle();
        return StringUtils.hasText(bundleName) ? bundleName : commonProperties.getOtlp().getSsl().getBundle();
    }

    /**
     * Get the SSL bundle to use for the connection to the endpoint, if any.
     */
    @Nullable
    public SslBundle getSslBundle(@Nullable SslBundles sslBundles) {
        String bundleName = getSslBundleName();
        if (!StringUtils.hasText(bundleName)) {
            return null;
        }
        Assert.state(sslBundles != null, "The SSL bundle '%s' is configured for the OTLP exporter, but no SSL bundles are available".formatted(bundleName));
        return sslBundles.getBundle(bundleName);
    }

    /**
     * Whether a proxy is configured explicitly, either for the signal or for all signals.
     */
    public boolean hasProxy() {
        return StringUtils.hasText(getProxyConfig().getHost());
    }

    /**
     * Get the proxy configured explicitly for the connection to the endpoint, if any. Without one, the HTTP
     * clients use the default proxy selector of the JVM, so that the standard proxy system properties apply,
     * such as {@code https.proxyHost}.
     */
    @Nullable
    public ProxyOptions getProxyOptions() {
        ProxyConfig proxy = getProxyConfig();
        String host = proxy.getHost();
        if (!StringUtils.hasText(host)) {
            return null;
        }
        Integer port = proxy.getPort();
        Assert.state(port != null && port > 0 && port <= 65535,
                "The OTLP exporter proxy '%s' requires a port between 1 and 65535, but it's %s. Set the port together with the host."
                        .formatted(host, port));
        return ProxyOptions.create(InetSocketAddress.createUnresolved(host, port));
    }

    private ProxyConfig getProxyConfig() {
        ProxyConfig signalProxy = signalProperties.getProxy();
        return StringUtils.hasText(signalProxy.getHost()) ? signalProxy : commonProperties.getOtlp().getProxy();
    }

    public boolean isMetrics() {
        Boolean metrics = signalProperties.isMetrics();
        return metrics != null ? metrics : commonProperties.getOtlp().isMetrics();
    }

    /**
     * Get the URL without the user information, query, and fragment, which may hold credentials,
     * so that it can be logged.
     */
    public static String toLoggableUrl(String url) {
        Assert.notNull(url, "url cannot be null");
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null) {
                return "<unparseable URL>";
            }
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
        } catch (URISyntaxException ex) {
            return "<unparseable URL>";
        }
    }

    /**
     * Get the TLS protocols the SSL bundle enables, if it restricts them.
     */
    @Nullable
    public static List<String> getEnabledProtocols(SslBundle sslBundle) {
        Assert.notNull(sslBundle, "sslBundle cannot be null");
        SslOptions options = sslBundle.getOptions();
        return options.getEnabledProtocols() != null ? List.of(options.getEnabledProtocols()) : null;
    }

    /**
     * Get the trust manager of the SSL bundle, which the OpenTelemetry exporters require together with the SSL context.
     */
    public static X509TrustManager getTrustManager(SslBundle sslBundle) {
        Assert.notNull(sslBundle, "sslBundle cannot be null");
        for (TrustManager trustManager : sslBundle.getManagers().getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("No X509TrustManager found in the trust managers of the SSL bundle");
    }

}
