package io.arconia.opentelemetry.exporter.sender.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.impl.InstrumentationUtil;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.export.Compressor;
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.common.export.HttpSenderConfig;
import io.opentelemetry.sdk.common.export.MessageWriter;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.common.export.RetryPolicy;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpClientSender}.
 */
class HttpClientSenderTests {

    private static final byte[] MESSAGE = "telemetry".getBytes(StandardCharsets.UTF_8);

    private final ConcurrentLinkedQueue<RecordedRequest> requests = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<ScriptedResponse> responses = new ConcurrentLinkedQueue<>();

    private HttpServer server;

    private final List<HttpSender> senders = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/traces", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            requests.add(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestHeaders(), body));
            ScriptedResponse response = responses.poll();
            if (response == null) {
                response = new ScriptedResponse(200, new byte[0], Map.of());
            }
            response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            exchange.sendResponseHeaders(response.status(), response.body().length == 0 ? -1 : response.body().length);
            if (response.body().length > 0) {
                exchange.getResponseBody().write(response.body());
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        senders.forEach(HttpSender::shutdown);
        server.stop(0);
    }

    @Nested
    class Requests {

        @Test
        void sendsMessageWithHeaders() throws Exception {
            HttpSender sender = sender(config().headers(() -> Map.of("Authorization", List.of("Bearer token"), "X-Values", List.of("a", "b"))));

            HttpResponse response = send(sender);

            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getStatusMessage()).isEqualTo("200");
            RecordedRequest request = requests.poll();
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.header("Content-Type")).isEqualTo("application/x-protobuf");
            assertThat(request.header("Accept-Encoding")).isEqualTo("gzip, identity");
            assertThat(request.header("Authorization")).isEqualTo("Bearer token");
            assertThat(request.headers().get("X-values")).containsExactly("a", "b");
            assertThat(request.header("Content-Encoding")).isNull();
            assertThat(request.body()).isEqualTo(MESSAGE);
        }

        @Test
        void compressesMessage() throws Exception {
            HttpSender sender = sender(config().compressor(new GzipCompressor()));

            send(sender);

            RecordedRequest request = requests.poll();
            assertThat(request.header("Content-Encoding")).isEqualTo("gzip");
            assertThat(new GZIPInputStream(new ByteArrayInputStream(request.body())).readAllBytes()).isEqualTo(MESSAGE);
        }

        @Test
        void requestsNoUpgradeToHttp2() throws Exception {
            send(sender(config()));

            assertThat(requests.poll().header("Upgrade")).isNull();
        }

        @Test
        void suppressesInstrumentationWhileSending() throws Exception {
            HttpSender sender = sender(config());
            CompletableFuture<Boolean> suppressed = new CompletableFuture<>();

            sender.send(new BytesMessageWriter(MESSAGE),
                    response -> suppressed.complete(InstrumentationUtil.shouldSuppressInstrumentation(Context.current())),
                    suppressed::completeExceptionally);

            assertThat(suppressed.get(5, TimeUnit.SECONDS)).isTrue();
        }

    }

    @Nested
    class Responses {

        @Test
        void decodesGzipResponse() throws Exception {
            responses.add(new ScriptedResponse(400, gzip("invalid".getBytes(StandardCharsets.UTF_8)), Map.of("Content-Encoding", "gzip")));

            HttpResponse response = send(sender(config()));

            assertThat(response.getStatusCode()).isEqualTo(400);
            assertThat(new String(response.getResponseBody(), StandardCharsets.UTF_8)).isEqualTo("invalid");
        }

        @Test
        void failsWithUnsupportedContentEncoding() {
            responses.add(new ScriptedResponse(200, "x".getBytes(StandardCharsets.UTF_8), Map.of("Content-Encoding", "br")));

            assertThat(sendFailure(sender(config()))).isInstanceOf(IOException.class).hasMessageContaining("Unsupported Content-Encoding");
            assertThat(requests).hasSize(1);
        }

        @Test
        void failsWhenResponseBodyExceedsLimit() {
            responses.add(new ScriptedResponse(200, new byte[11], Map.of()));

            assertThat(sendFailure(sender(config().maxResponseBodySize(10)))).isInstanceOf(IOException.class).hasMessageContaining("exceeds the limit of 10 bytes");
        }

        @Test
        void acceptsResponseBodyAtLimit() throws Exception {
            responses.add(new ScriptedResponse(200, new byte[10], Map.of()));

            assertThat(send(sender(config().maxResponseBodySize(10))).getResponseBody()).hasSize(10);
        }

    }

    @Nested
    class Retries {

        @Test
        void retriesRetryableStatus() throws Exception {
            responses.add(new ScriptedResponse(503, new byte[0], Map.of()));
            responses.add(new ScriptedResponse(429, new byte[0], Map.of()));

            HttpResponse response = send(sender(config().retryPolicy(retryPolicy(5, Duration.ofMillis(10)))));

            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(requests).hasSize(3);
        }

        @Test
        void doesNotRetryOtherStatus() throws Exception {
            responses.add(new ScriptedResponse(400, new byte[0], Map.of()));

            assertThat(send(sender(config().retryPolicy(retryPolicy(5, Duration.ofMillis(10))))).getStatusCode()).isEqualTo(400);
            assertThat(requests).hasSize(1);
        }

        @Test
        void returnsLastResponseAfterMaxAttempts() throws Exception {
            for (int i = 0; i < 5; i++) {
                responses.add(new ScriptedResponse(503, new byte[0], Map.of()));
            }

            assertThat(send(sender(config().retryPolicy(retryPolicy(3, Duration.ofMillis(10))))).getStatusCode()).isEqualTo(503);
            assertThat(requests).hasSize(3);
        }

        @Test
        void doesNotRetryWithoutRetryPolicy() throws Exception {
            responses.add(new ScriptedResponse(503, new byte[0], Map.of()));

            assertThat(send(sender(config())).getStatusCode()).isEqualTo(503);
            assertThat(requests).hasSize(1);
        }

        @Test
        void stopsWhenBackoffExceedsTimeout() throws Exception {
            responses.add(new ScriptedResponse(503, new byte[0], Map.of()));

            HttpResponse response = send(sender(config().timeout(Duration.ofSeconds(1)).retryPolicy(retryPolicy(5, Duration.ofSeconds(5)))));

            assertThat(response.getStatusCode()).isEqualTo(503);
            assertThat(requests).hasSize(1);
        }

        @Test
        void honorsRetryAfter() throws Exception {
            // Without the Retry-After header, the backoff would exceed the timeout and stop the retries.
            responses.add(new ScriptedResponse(429, new byte[0], Map.of("Retry-After", "0")));

            HttpResponse response = send(sender(config().timeout(Duration.ofSeconds(2)).retryPolicy(retryPolicy(5, Duration.ofSeconds(5)))));

            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(requests).hasSize(2);
        }

        @Test
        void retriesRetryableFailureUntilMaxAttempts() {
            AtomicInteger failures = new AtomicInteger();
            RetryPolicy retryPolicy = retryPolicy(3, Duration.ofMillis(10)).toBuilder()
                    .setRetryExceptionPredicate(exception -> failures.incrementAndGet() > 0)
                    .build();

            Throwable failure = sendFailure(sender(config().endpoint(unusedEndpoint()).retryPolicy(retryPolicy)));

            assertThat(failure).isInstanceOf(ConnectException.class);
            assertThat(failures).hasValue(3);
        }

        @Test
        void doesNotRetryNonRetryableFailure() {
            AtomicInteger failures = new AtomicInteger();
            RetryPolicy retryPolicy = retryPolicy(3, Duration.ofMillis(10)).toBuilder()
                    .setRetryExceptionPredicate(exception -> failures.incrementAndGet() < 0)
                    .build();

            assertThat(sendFailure(sender(config().endpoint(unusedEndpoint()).retryPolicy(retryPolicy)))).isInstanceOf(ConnectException.class);
            assertThat(failures).hasValue(1);
        }

        @Test
        void defaultRetryableFailures() {
            assertThat(HttpClientSender.isRetryable(new ConnectException("refused"))).isTrue();
            assertThat(HttpClientSender.isRetryable(new IOException("reset"))).isTrue();
            assertThat(HttpClientSender.isRetryable(new SSLException("handshake"))).isFalse();
        }

        @Test
        void parsesRetryAfter() {
            Instant now = Instant.parse("2026-11-02T10:00:00Z");
            String inTenSeconds = DateTimeFormatter.RFC_1123_DATE_TIME.format(now.plusSeconds(10).atZone(ZoneOffset.UTC));
            String tenSecondsAgo = DateTimeFormatter.RFC_1123_DATE_TIME.format(now.minusSeconds(10).atZone(ZoneOffset.UTC));

            assertThat(HttpClientSender.retryAfterNanos("3", now)).hasValue(TimeUnit.SECONDS.toNanos(3));
            assertThat(HttpClientSender.retryAfterNanos(inTenSeconds, now)).hasValue(TimeUnit.SECONDS.toNanos(10));
            assertThat(HttpClientSender.retryAfterNanos(tenSecondsAgo, now)).hasValue(0);
            assertThat(HttpClientSender.retryAfterNanos("-1", now)).isEmpty();
            assertThat(HttpClientSender.retryAfterNanos("soon", now)).isEmpty();
            assertThat(HttpClientSender.retryAfterNanos(null, now)).isEmpty();
        }

    }

    @Nested
    class Lifecycle {

        @Test
        void failsWhenExecutorRejectsExport() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.shutdown();

            assertThat(sendFailure(sender(config().executor(executor)))).isInstanceOf(RejectedExecutionException.class);
        }

        @Test
        void shutdownLeavesProvidedExecutorRunning() throws Exception {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                HttpSender sender = new HttpClientSenderProvider().createSender(config().executor(executor));
                send(sender);

                assertThat(sender.shutdown().join(10, TimeUnit.SECONDS).isSuccess()).isTrue();
                assertThat(executor.isShutdown()).isFalse();
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        void shutdownStopsManagedExecutor() throws Exception {
            HttpSender sender = new HttpClientSenderProvider().createSender(config());
            send(sender);

            assertThat(sender.shutdown().join(10, TimeUnit.SECONDS).isSuccess()).isTrue();
            assertThat(sendFailure(sender)).isInstanceOf(RejectedExecutionException.class);
        }

    }

    private HttpSender sender(TestConfig config) {
        HttpSender sender = new HttpClientSenderProvider().createSender(config);
        senders.add(sender);
        return sender;
    }

    private HttpResponse send(HttpSender sender) throws Exception {
        CompletableFuture<HttpResponse> result = new CompletableFuture<>();
        sender.send(new BytesMessageWriter(MESSAGE), result::complete, result::completeExceptionally);
        return result.get(30, TimeUnit.SECONDS);
    }

    private Throwable sendFailure(HttpSender sender) {
        CompletableFuture<Throwable> result = new CompletableFuture<>();
        sender.send(new BytesMessageWriter(MESSAGE), response -> result.complete(null), result::complete);
        return result.join();
    }

    private TestConfig config() {
        return new TestConfig(URI.create("http://localhost:%d/v1/traces".formatted(server.getAddress().getPort())));
    }

    private static URI unusedEndpoint() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return URI.create("http://localhost:%d/v1/traces".formatted(socket.getLocalPort()));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static RetryPolicy retryPolicy(int maxAttempts, Duration initialBackoff) {
        return RetryPolicy.builder()
                .setMaxAttempts(maxAttempts)
                .setInitialBackoff(initialBackoff)
                .setMaxBackoff(initialBackoff.multipliedBy(4))
                .setBackoffMultiplier(1.5)
                .build();
    }

    private static byte[] gzip(byte[] bytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(bytes);
        }
        return output.toByteArray();
    }

    private record RecordedRequest(String method, Map<String, List<String>> headers, byte[] body) {

        @Nullable
        String header(String name) {
            List<String> values = headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            return values != null ? values.get(0) : null;
        }

    }

    private record ScriptedResponse(int status, byte[] body, Map<String, String> headers) {
    }

    private record BytesMessageWriter(byte[] bytes) implements MessageWriter {

        @Override
        public void writeMessage(OutputStream output) throws IOException {
            output.write(bytes);
        }

        @Override
        public int getContentLength() {
            return bytes.length;
        }

    }

    private static final class GzipCompressor implements Compressor {

        @Override
        public String getEncoding() {
            return "gzip";
        }

        @Override
        public OutputStream compress(OutputStream outputStream) throws IOException {
            return new GZIPOutputStream(outputStream);
        }

    }

    /**
     * The configuration of a sender, as the OpenTelemetry exporters provide it.
     */
    private static final class TestConfig implements HttpSenderConfig {

        private URI endpoint;

        @Nullable
        private Compressor compressor;

        private Duration timeout = Duration.ofSeconds(10);

        private Supplier<Map<String, List<String>>> headers = Collections::emptyMap;

        @Nullable
        private RetryPolicy retryPolicy;

        @Nullable
        private ExecutorService executor;

        private long maxResponseBodySize = 4 * 1024 * 1024;

        TestConfig(URI endpoint) {
            this.endpoint = endpoint;
        }

        TestConfig endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        TestConfig compressor(Compressor compressor) {
            this.compressor = compressor;
            return this;
        }

        TestConfig timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        TestConfig headers(Supplier<Map<String, List<String>>> headers) {
            this.headers = headers;
            return this;
        }

        TestConfig retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        TestConfig executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        TestConfig maxResponseBodySize(long maxResponseBodySize) {
            this.maxResponseBodySize = maxResponseBodySize;
            return this;
        }

        @Override
        public URI getEndpoint() {
            return endpoint;
        }

        @Override
        public String getContentType() {
            return "application/x-protobuf";
        }

        @Override
        public @Nullable Compressor getCompressor() {
            return compressor;
        }

        @Override
        public Duration getTimeout() {
            return timeout;
        }

        @Override
        public Duration getConnectTimeout() {
            return Duration.ofSeconds(1);
        }

        @Override
        public Supplier<Map<String, List<String>>> getHeadersSupplier() {
            return headers;
        }

        @Override
        public @Nullable ProxyOptions getProxyOptions() {
            return null;
        }

        @Override
        public @Nullable RetryPolicy getRetryPolicy() {
            return retryPolicy;
        }

        @Override
        public @Nullable SSLContext getSslContext() {
            return null;
        }

        @Override
        public @Nullable X509TrustManager getTrustManager() {
            return null;
        }

        @Override
        public @Nullable ExecutorService getExecutorService() {
            return executor;
        }

        @Override
        public long getMaxResponseBodySize() {
            return maxResponseBodySize;
        }

    }

}
