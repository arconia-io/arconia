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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import io.micrometer.core.ipc.http.HttpSender;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpHttpClients;

/**
 * {@link HttpSender} based on the JDK {@link HttpClient}, so that the connection
 * to the OTLP endpoint can be secured with an SSL bundle and go through a proxy. The client is
 * set up as for the OpenTelemetry SDK exporters, see {@link OtlpHttpClients}.
 */
final class JdkHttpClientSender implements HttpSender {

    private final HttpClient httpClient;

    private final Duration timeout;

    JdkHttpClientSender(URI endpoint, Duration connectTimeout, Duration timeout, @Nullable SslBundle sslBundle, @Nullable ProxySelector proxySelector) {
        Assert.notNull(endpoint, "endpoint cannot be null");
        Assert.notNull(connectTimeout, "connectTimeout cannot be null");
        Assert.notNull(timeout, "timeout cannot be null");
        SSLContext sslContext = null;
        SSLParameters sslParameters = null;
        if (sslBundle != null) {
            sslContext = sslBundle.createSslContext();
            SslOptions options = sslBundle.getOptions();
            if (options.isSpecified()) {
                sslParameters = new SSLParameters();
                sslParameters.setCipherSuites(options.getCiphers());
                sslParameters.setProtocols(options.getEnabledProtocols());
            }
        }
        this.httpClient = OtlpHttpClients.create(endpoint, connectTimeout, sslContext, sslParameters, proxySelector);
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
