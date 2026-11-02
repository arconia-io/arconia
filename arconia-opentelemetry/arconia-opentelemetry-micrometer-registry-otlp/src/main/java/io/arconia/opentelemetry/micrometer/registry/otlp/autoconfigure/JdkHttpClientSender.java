package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import javax.net.ssl.SSLParameters;

import io.micrometer.core.ipc.http.HttpSender;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;

/**
 * {@link HttpSender} based on the JDK {@link HttpClient}, so that the connection
 * to the OTLP endpoint can be secured with an SSL bundle and go through a proxy.
 */
final class JdkHttpClientSender implements HttpSender {

    private final HttpClient httpClient;

    private final Duration timeout;

    JdkHttpClientSender(Duration connectTimeout, Duration timeout, @Nullable SslBundle sslBundle, @Nullable ProxySelector proxySelector) {
        Assert.notNull(connectTimeout, "connectTimeout cannot be null");
        Assert.notNull(timeout, "timeout cannot be null");
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                // By default, the client asks to upgrade cleartext connections to HTTP/2 (h2c), which many OTLP
                // backends and proxies reject by closing the connection. OTLP works the same over HTTP/1.1.
                // See https://github.com/open-telemetry/opentelemetry-java/issues/8982.
                .version(HttpClient.Version.HTTP_1_1);
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        if (sslBundle != null) {
            builder.sslContext(sslBundle.createSslContext());
            SslOptions options = sslBundle.getOptions();
            if (options.isSpecified()) {
                SSLParameters parameters = new SSLParameters();
                parameters.setCipherSuites(options.getCiphers());
                parameters.setProtocols(options.getEnabledProtocols());
                builder.sslParameters(parameters);
            }
        }
        this.httpClient = builder.build();
        this.timeout = timeout;
    }

    @Override
    public Response send(Request request) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.getUrl().toString()))
                .timeout(timeout)
                .method(request.getMethod().name(), BodyPublishers.ofByteArray(request.getEntity()));
        request.getRequestHeaders().forEach(builder::header);
        HttpResponse<String> response = httpClient.send(builder.build(), BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

}
