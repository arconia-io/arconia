package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link OtlpHttpClients}.
 */
class OtlpHttpClientsTests {

    private static final URI HTTPS_ENDPOINT = URI.create("https://otlp.example.com:4318/v1/traces");

    private static final URI HTTP_ENDPOINT = URI.create("http://localhost:4318/v1/traces");

    @Test
    void usesHttp11ByDefault() {
        assertThat(OtlpHttpClients.create(HTTPS_ENDPOINT, Duration.ofSeconds(1), null, null, null).version())
                .isEqualTo(HttpClient.Version.HTTP_1_1);
        assertThat(OtlpHttpClients.create(HTTP_ENDPOINT, Duration.ofSeconds(1), null, null, null).version())
                .isEqualTo(HttpClient.Version.HTTP_1_1);
    }

    @Test
    void negotiatesHttp2OnlyOverTlsWhenEnabled() {
        assertThat(OtlpHttpClients.version(HTTPS_ENDPOINT, true)).isEqualTo(HttpClient.Version.HTTP_2);
        assertThat(OtlpHttpClients.version(HTTP_ENDPOINT, true)).isEqualTo(HttpClient.Version.HTTP_1_1);
        assertThat(OtlpHttpClients.version(HTTPS_ENDPOINT, false)).isEqualTo(HttpClient.Version.HTTP_1_1);
    }

    @Test
    void appliesConnectionSettings() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setProtocols(new String[] { "TLSv1.3" });
        ProxySelector proxySelector = ProxySelector.of(InetSocketAddress.createUnresolved("proxy.example.com", 3128));

        HttpClient httpClient = OtlpHttpClients.create(HTTPS_ENDPOINT, Duration.ofSeconds(3), sslContext, sslParameters, proxySelector);

        assertThat(httpClient.connectTimeout()).contains(Duration.ofSeconds(3));
        assertThat(httpClient.sslContext()).isSameAs(sslContext);
        assertThat(httpClient.sslParameters().getProtocols()).containsExactly("TLSv1.3");
        assertThat(httpClient.proxy()).contains(proxySelector);
    }

    @Test
    void whenEndpointNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OtlpHttpClients.create(null, Duration.ofSeconds(1), null, null, null))
                .withMessage("endpoint cannot be null");
    }

}
