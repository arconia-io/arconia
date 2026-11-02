package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.ipc.http.HttpSender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link JdkHttpClientSender}.
 */
class JdkHttpClientSenderTests {

    private final AtomicReference<String> receivedMethod = new AtomicReference<>();

    private final AtomicReference<String> receivedHeader = new AtomicReference<>();

    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/metrics", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedHeader.set(exchange.getRequestHeaders().getFirst("X-Tenant"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "accepted".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(202, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void whenConnectTimeoutNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JdkHttpClientSender(null, Duration.ofSeconds(10), null))
                .withMessage("connectTimeout cannot be null");
    }

    @Test
    void whenTimeoutNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JdkHttpClientSender(Duration.ofSeconds(10), null, null))
                .withMessage("timeout cannot be null");
    }

    @Test
    void sendsRequest() throws Throwable {
        JdkHttpClientSender sender = new JdkHttpClientSender(Duration.ofSeconds(1), Duration.ofSeconds(5), null);
        URI uri = URI.create("http://localhost:%d/v1/metrics".formatted(server.getAddress().getPort()));

        HttpSender.Response response = sender.send(new HttpSender.Request(uri.toURL(),
                "metrics".getBytes(StandardCharsets.UTF_8), HttpSender.Method.POST, Map.of("X-Tenant", "acme")));

        assertThat(response.code()).isEqualTo(202);
        assertThat(response.body()).isEqualTo("accepted");
        assertThat(receivedMethod.get()).isEqualTo("POST");
        assertThat(receivedHeader.get()).isEqualTo("acme");
        assertThat(receivedBody.get()).isEqualTo("metrics");
    }

    @Test
    void usesHttp11() {
        JdkHttpClientSender sender = new JdkHttpClientSender(Duration.ofSeconds(1), Duration.ofSeconds(5), null);

        HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(sender, "httpClient");
        assertThat(httpClient.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
    }

    @Test
    void sendsNoUpgradeRequestToCleartextEndpoint() throws Throwable {
        AtomicReference<String> upgradeHeader = new AtomicReference<>();
        server.createContext("/v1/upgrade", exchange -> {
            upgradeHeader.set(String.valueOf(exchange.getRequestHeaders().getFirst("Upgrade")));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        JdkHttpClientSender sender = new JdkHttpClientSender(Duration.ofSeconds(1), Duration.ofSeconds(5), null);
        URI uri = URI.create("http://localhost:%d/v1/upgrade".formatted(server.getAddress().getPort()));

        sender.send(new HttpSender.Request(uri.toURL(), new byte[0], HttpSender.Method.POST, Map.of()));

        assertThat(upgradeHeader.get()).isEqualTo("null");
    }

    @Test
    void usesSslBundle() throws Exception {
        SslBundle sslBundle = SslBundle.of(new PemSslStoreBundle(null, PemSslStoreDetails.forCertificate("classpath:ssl/ca.crt")),
                null, SslOptions.of(null, new String[] { "TLSv1.3" }));

        JdkHttpClientSender sender = new JdkHttpClientSender(Duration.ofSeconds(1), Duration.ofSeconds(5), sslBundle);

        HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(sender, "httpClient");
        assertThat(httpClient.sslContext()).isNotSameAs(SSLContext.getDefault());
        assertThat(httpClient.sslParameters().getProtocols()).containsExactly("TLSv1.3");
    }

}
