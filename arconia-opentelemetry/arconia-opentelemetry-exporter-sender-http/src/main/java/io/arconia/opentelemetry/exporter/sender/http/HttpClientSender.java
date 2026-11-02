package io.arconia.opentelemetry.exporter.sender.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLException;

import io.opentelemetry.api.impl.InstrumentationUtil;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.Compressor;
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.common.export.HttpSenderConfig;
import io.opentelemetry.sdk.common.export.MessageWriter;
import io.opentelemetry.sdk.common.export.RetryPolicy;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * {@link HttpSender} based on the JDK {@link HttpClient}. Each export is sent on a thread of the executor,
 * with the instrumentation of the request suppressed, and retried according to the {@link RetryPolicy},
 * as long as the export timeout allows.
 */
final class HttpClientSender implements HttpSender {

    /**
     * The HTTP status codes of a response worth retrying, as defined by the OTLP specification.
     */
    static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);

    /**
     * The shortest timeout of a request, for an attempt starting right before the export timeout.
     */
    private static final Duration MIN_REQUEST_TIMEOUT = Duration.ofMillis(1);

    private static final Logger logger = LoggerFactory.getLogger(HttpClientSender.class);

    private final HttpClient httpClient;

    private final URI endpoint;

    private final String contentType;

    @Nullable
    private final Compressor compressor;

    private final Duration timeout;

    private final Supplier<Map<String, List<String>>> headersSupplier;

    @Nullable
    private final RetryPolicy retryPolicy;

    private final Predicate<IOException> retryableException;

    private final ExecutorService executor;

    private final boolean managedExecutor;

    private final long maxResponseBodySize;

    HttpClientSender(HttpClient httpClient, HttpSenderConfig config) {
        Assert.notNull(httpClient, "httpClient cannot be null");
        Assert.notNull(config, "config cannot be null");
        this.httpClient = httpClient;
        this.endpoint = config.getEndpoint();
        this.contentType = config.getContentType();
        this.compressor = config.getCompressor();
        this.timeout = config.getTimeout();
        this.headersSupplier = config.getHeadersSupplier();
        this.retryPolicy = config.getRetryPolicy();
        Predicate<IOException> customRetryableException = retryPolicy != null ? retryPolicy.getRetryExceptionPredicate() : null;
        this.retryableException = customRetryableException != null ? customRetryableException : HttpClientSender::isRetryable;
        ExecutorService configuredExecutor = config.getExecutorService();
        this.managedExecutor = configuredExecutor == null;
        this.executor = configuredExecutor != null ? configuredExecutor : newExecutor();
        this.maxResponseBodySize = config.getMaxResponseBodySize();
    }

    /**
     * A bounded pool of daemon threads, created only when needed, for exporters that provide no executor.
     */
    private static ExecutorService newExecutor() {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("otlp-http-sender-");
        threadFactory.setDaemon(true);
        return new ThreadPoolExecutor(0, Math.max(Runtime.getRuntime().availableProcessors(), 5),
                60, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
    }

    @Override
    public void send(MessageWriter messageWriter, Consumer<HttpResponse> onResponse, Consumer<Throwable> onError) {
        try {
            // The suppression applies on the thread sending the request, so that its instrumentation,
            // if any, doesn't produce telemetry about the export itself.
            executor.execute(() -> InstrumentationUtil.suppressInstrumentation(() -> {
                HttpResponse response;
                try {
                    response = sendWithRetries(messageWriter);
                } catch (Throwable ex) {
                    // Any failure completes the export, which would otherwise wait for it forever.
                    onError.accept(ex);
                    return;
                }
                onResponse.accept(response);
            }));
        } catch (RejectedExecutionException ex) {
            onError.accept(ex);
        }
    }

    /**
     * Sends the request and retries it while the response or the failure is retryable, the retry policy
     * allows another attempt, and the backoff ends before the export timeout. The last response is returned,
     * or the last failure thrown, when no further attempt is made.
     */
    HttpResponse sendWithRetries(MessageWriter messageWriter) throws IOException {
        long startNanos = System.nanoTime();
        HttpRequest.Builder request = newRequest(messageWriter);
        if (retryPolicy == null) {
            return sendOnce(request, timeout);
        }

        int attempts = 0;
        long backoffNanos = retryPolicy.getInitialBackoff().toNanos();
        while (true) {
            Duration remaining = timeout.minusNanos(System.nanoTime() - startNanos);
            if (remaining.compareTo(MIN_REQUEST_TIMEOUT) < 0) {
                remaining = MIN_REQUEST_TIMEOUT;
            }
            HttpResponse response = null;
            IOException failure = null;
            OptionalLong retryAfterNanos = OptionalLong.empty();
            try {
                java.net.http.HttpResponse<InputStream> rawResponse = send(request, remaining);
                response = toHttpResponse(rawResponse);
                if (!RETRYABLE_STATUS_CODES.contains(rawResponse.statusCode())) {
                    return response;
                }
                retryAfterNanos = retryAfterNanos(rawResponse.headers().firstValue("Retry-After").orElse(null), Instant.now());
                logger.debug("OTLP export returned retryable status {}", rawResponse.statusCode());
            } catch (IOException ex) {
                if (!retryableException.test(ex)) {
                    throw ex;
                }
                failure = ex;
                logger.debug("OTLP export failed with a retryable exception", ex);
            }

            attempts++;
            long cappedBackoffNanos = Math.min(backoffNanos, retryPolicy.getMaxBackoff().toNanos());
            long delayNanos = retryAfterNanos.orElse((long) (cappedBackoffNanos * ThreadLocalRandom.current().nextDouble(0.8, 1.2)));
            long remainingNanos = timeout.toNanos() - (System.nanoTime() - startNanos);
            if (attempts >= retryPolicy.getMaxAttempts() || delayNanos >= remainingNanos || !sleep(delayNanos)) {
                return lastResult(response, failure);
            }
            backoffNanos = (long) (cappedBackoffNanos * retryPolicy.getBackoffMultiplier());
        }
    }

    private HttpRequest.Builder newRequest(MessageWriter messageWriter) throws IOException {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint);
        headersSupplier.get().forEach((name, values) -> values.forEach(value -> request.header(name, value)));
        request.header("Content-Type", contentType);
        request.header("Accept-Encoding", "gzip, identity");

        RequestBody body = new RequestBody(Math.max(messageWriter.getContentLength(), 32));
        if (compressor != null) {
            request.header("Content-Encoding", compressor.getEncoding());
            try (OutputStream compressed = compressor.compress(body)) {
                messageWriter.writeMessage(compressed);
            }
        } else {
            messageWriter.writeMessage(body);
        }
        return request.POST(body.publisher());
    }

    private HttpResponse sendOnce(HttpRequest.Builder request, Duration requestTimeout) throws IOException {
        return toHttpResponse(send(request, requestTimeout));
    }

    private java.net.http.HttpResponse<InputStream> send(HttpRequest.Builder request, Duration requestTimeout) throws IOException {
        try {
            return httpClient.send(request.timeout(requestTimeout).build(), BodyHandlers.ofInputStream());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending the OTLP export", ex);
        }
    }

    /**
     * Reads the response body, decompressed if needed, up to the configured limit. A body that can't be read
     * is reported as empty, since the server has already processed the request and sending it again would
     * duplicate the telemetry.
     */
    private HttpResponse toHttpResponse(java.net.http.HttpResponse<InputStream> response) throws IOException {
        String contentEncoding = response.headers().firstValue("Content-Encoding").orElse("identity");
        boolean gzip = "gzip".equalsIgnoreCase(contentEncoding);
        // Reading one byte more than the limit detects a body exceeding it.
        int readLimit = maxResponseBodySize >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxResponseBodySize + 1;
        byte[] bytes;
        try (InputStream rawBody = response.body()) {
            if (!gzip && !"identity".equalsIgnoreCase(contentEncoding)) {
                throw new UnsupportedContentEncodingException("Unsupported Content-Encoding in the OTLP response: " + contentEncoding);
            }
            InputStream body = gzip ? new GZIPInputStream(rawBody) : rawBody;
            bytes = body.readNBytes(readLimit);
        } catch (UnsupportedContentEncodingException ex) {
            throw ex;
        } catch (IOException ex) {
            logger.warn("Failed to read the body of the OTLP response", ex);
            bytes = new byte[0];
        }
        if (bytes.length > maxResponseBodySize) {
            throw new ResponseBodyTooLargeException("The OTLP response body exceeds the limit of " + maxResponseBodySize + " bytes");
        }
        return new SimpleHttpResponse(response.statusCode(), bytes);
    }

    private static HttpResponse lastResult(@Nullable HttpResponse response, @Nullable IOException failure) throws IOException {
        if (response != null) {
            return response;
        }
        Assert.state(failure != null, "Either a response or a failure is expected");
        throw failure;
    }

    /**
     * Sleeps for the given time, returning {@code false} if interrupted.
     */
    private static boolean sleep(long nanos) {
        try {
            TimeUnit.NANOSECONDS.sleep(nanos);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Most failures sending a request are transient, such as a reset connection or a timeout. The ones that
     * can't be resolved by retrying are excluded.
     */
    static boolean isRetryable(IOException exception) {
        return !(exception instanceof SSLException)
                && !(exception instanceof ResponseBodyTooLargeException)
                && !(exception instanceof UnsupportedContentEncodingException);
    }

    /**
     * Parses the value of a {@code Retry-After} header, either a number of seconds or an HTTP date.
     */
    static OptionalLong retryAfterNanos(@Nullable String retryAfter, Instant now) {
        if (retryAfter == null) {
            return OptionalLong.empty();
        }
        try {
            long seconds = Long.parseLong(retryAfter.strip());
            return seconds >= 0 ? OptionalLong.of(TimeUnit.SECONDS.toNanos(seconds)) : OptionalLong.empty();
        } catch (NumberFormatException ex) {
            // Not a number of seconds, so it could be a date.
        }
        try {
            Instant retryAt = ZonedDateTime.parse(retryAfter.strip(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            return OptionalLong.of(Math.max(0, Duration.between(now, retryAt).toNanos()));
        } catch (DateTimeParseException | ArithmeticException ex) {
            return OptionalLong.empty();
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        if (managedExecutor) {
            // Interrupts in-flight requests and backoffs.
            executor.shutdownNow();
        }
        // Closing the client waits for in-flight requests, so it happens off the calling thread.
        CompletableResultCode result = new CompletableResultCode();
        Thread shutdownThread = new Thread(() -> {
            try {
                if (managedExecutor && !executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("The OTLP HTTP sender threads didn't terminate within 5 seconds");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                // The client is closed even if waiting for the threads was cut short.
                try {
                    httpClient.close();
                    result.succeed();
                } catch (RuntimeException ex) {
                    result.failExceptionally(ex);
                }
            }
        }, "otlp-http-sender-shutdown");
        shutdownThread.setDaemon(true);
        shutdownThread.start();
        return result;
    }

    /**
     * The request body, published without copying the bytes written to it.
     */
    private static final class RequestBody extends ByteArrayOutputStream {

        RequestBody(int initialSize) {
            super(initialSize);
        }

        HttpRequest.BodyPublisher publisher() {
            return BodyPublishers.ofByteArray(buf, 0, count);
        }

    }

    private record SimpleHttpResponse(int statusCode, byte[] body) implements HttpResponse {

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String getStatusMessage() {
            return String.valueOf(statusCode);
        }

        @Override
        public byte[] getResponseBody() {
            return body;
        }

    }

    private static final class ResponseBodyTooLargeException extends IOException {

        ResponseBodyTooLargeException(String message) {
            super(message);
        }

    }

    private static final class UnsupportedContentEncodingException extends IOException {

        UnsupportedContentEncodingException(String message) {
            super(message);
        }

    }

}
