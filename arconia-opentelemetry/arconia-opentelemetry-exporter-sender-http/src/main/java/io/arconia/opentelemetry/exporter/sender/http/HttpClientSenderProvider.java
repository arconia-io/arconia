package io.arconia.opentelemetry.exporter.sender.http;

import java.net.http.HttpClient;
import java.util.List;
import java.util.ServiceLoader;

import javax.net.ssl.SSLParameters;

import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.common.export.HttpSenderConfig;
import io.opentelemetry.sdk.common.export.HttpSenderProvider;
import io.opentelemetry.sdk.common.export.ProxyOptions;

import org.springframework.util.CollectionUtils;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpHttpClients;

/**
 * {@link HttpSenderProvider} for the OpenTelemetry OTLP/HTTP exporters, sending requests with the JDK
 * {@link HttpClient}, over HTTP/1.1 by default.
 * <p>
 * The OpenTelemetry SDK finds it through the {@link ServiceLoader}. To use another sender instead,
 * such as the OkHttp one, exclude this module. When several senders are on the classpath, select one by setting
 * the {@code io.opentelemetry.sdk.common.export.HttpSenderProvider} system property to the fully qualified name
 * of its provider.
 */
public final class HttpClientSenderProvider implements HttpSenderProvider {

    @Override
    public HttpSender createSender(HttpSenderConfig config) {
        SSLParameters sslParameters = null;
        List<String> enabledProtocols = config.getEnabledProtocols();
        if (!CollectionUtils.isEmpty(enabledProtocols)) {
            sslParameters = new SSLParameters();
            sslParameters.setProtocols(enabledProtocols.toArray(new String[0]));
        }
        ProxyOptions proxyOptions = config.getProxyOptions();
        return new HttpClientSender(OtlpHttpClients.create(config.getEndpoint(), config.getConnectTimeout(), config.getSslContext(),
                sslParameters, proxyOptions != null ? proxyOptions.getProxySelector() : null), config);
    }

}
