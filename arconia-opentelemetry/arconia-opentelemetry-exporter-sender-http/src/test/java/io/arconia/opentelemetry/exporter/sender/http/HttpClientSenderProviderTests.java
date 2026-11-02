package io.arconia.opentelemetry.exporter.sender.http;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.common.export.HttpSenderProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpClientSenderProvider}.
 */
class HttpClientSenderProviderTests {

    @Test
    void isDiscoveredByServiceLoader() {
        assertThat(ServiceLoader.load(HttpSenderProvider.class)).singleElement()
                .isInstanceOf(HttpClientSenderProvider.class);
    }

    @Test
    void exportsSpansOverHttp11() throws Exception {
        ConcurrentLinkedQueue<Map<String, List<String>>> requests = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> protocols = new ConcurrentLinkedQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/traces", exchange -> {
            exchange.getRequestBody().readAllBytes();
            requests.add(new Headers(exchange.getRequestHeaders()));
            protocols.add(exchange.getProtocol());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint("http://localhost:%d/v1/traces".formatted(server.getAddress().getPort()))
                    .build();
            try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                    .build()) {
                tracerProvider.get("test").spanBuilder("span").startSpan().end();

                assertThat(tracerProvider.forceFlush().join(10, TimeUnit.SECONDS).isSuccess()).isTrue();
            }
        } finally {
            server.stop(0);
        }

        assertThat(protocols).containsExactly("HTTP/1.1");
        Headers headers = new Headers(requests.poll());
        assertThat(headers.getFirst("Content-Type")).isEqualTo("application/x-protobuf");
        assertThat(headers.containsKey("Upgrade")).isFalse();
    }

}
