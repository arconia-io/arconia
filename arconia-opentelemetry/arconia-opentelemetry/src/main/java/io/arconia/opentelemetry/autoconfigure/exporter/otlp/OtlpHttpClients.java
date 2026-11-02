package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Internal;

/**
 * Creates the JDK {@link HttpClient} used to send telemetry to an OTLP endpoint over HTTP.
 * <p>
 * The client uses HTTP/1.1, which every OTLP endpoint supports. By default, the JDK client asks plaintext
 * servers to upgrade to HTTP/2 (h2c), which many servers and proxies reject by closing the connection, and it
 * negotiates HTTP/2 over TLS, which load balancers may forward to the backend as h2c. Since each export is a
 * single request, HTTP/2 brings little to OTLP.
 * <p>
 * Experimental and undocumented: setting the {@value #HTTP2_PROPERTY} system property or the
 * {@value #HTTP2_ENVIRONMENT_VARIABLE} environment variable to {@code true} lets the client negotiate HTTP/2
 * with endpoints using TLS. Plaintext endpoints always use HTTP/1.1.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-java/issues/8982">opentelemetry-java#8982</a>
 */
@Internal
public final class OtlpHttpClients {

    static final String HTTP2_PROPERTY = "arconia.otel.exporter.otlp.http2";

    static final String HTTP2_ENVIRONMENT_VARIABLE = "ARCONIA_OTEL_EXPORTER_OTLP_HTTP2";

    private static final Logger logger = LoggerFactory.getLogger(OtlpHttpClients.class);

    private OtlpHttpClients() {
    }

    public static HttpClient create(URI endpoint, Duration connectTimeout, @Nullable SSLContext sslContext,
            @Nullable SSLParameters sslParameters, @Nullable ProxySelector proxySelector) {
        Assert.notNull(endpoint, "endpoint cannot be null");
        Assert.notNull(connectTimeout, "connectTimeout cannot be null");
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(version(endpoint, isHttp2Enabled()))
                .connectTimeout(connectTimeout);
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        if (sslParameters != null) {
            builder.sslParameters(sslParameters);
        }
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        return builder.build();
    }

    static HttpClient.Version version(URI endpoint, boolean http2Enabled) {
        if (http2Enabled && "https".equalsIgnoreCase(endpoint.getScheme())) {
            logger.info("Negotiating HTTP/2 with the OTLP endpoint {} (experimental)", EffectiveOtlpExporterConfig.toLoggableUrl(endpoint.toString()));
            return HttpClient.Version.HTTP_2;
        }
        return HttpClient.Version.HTTP_1_1;
    }

    private static boolean isHttp2Enabled() {
        String value = System.getProperty(HTTP2_PROPERTY);
        return Boolean.parseBoolean(value != null ? value : System.getenv(HTTP2_ENVIRONMENT_VARIABLE));
    }

}
