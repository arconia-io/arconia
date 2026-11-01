package io.arconia.docling.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.clear.request.ClearConvertersRequest;
import ai.docling.serve.api.clear.request.ClearResultsRequest;
import ai.docling.serve.api.clear.response.ClearResponse;
import ai.docling.serve.api.convert.request.BatchConvertDocumentRequest;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.health.HealthCheckResponse;
import ai.docling.serve.api.task.request.TaskResultRequest;
import ai.docling.serve.api.task.request.TaskStatusPollRequest;
import ai.docling.serve.api.task.response.TaskStatus;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import io.arconia.docling.autoconfigure.DoclingProperties;

/**
 * Client interface for interacting with the Docling Serve API.
 */
public class DoclingServeClient implements DoclingServeApi {

    private final RestClient restClient;
    private final DoclingServeApiConfig config;

    private DoclingServeClient(RestClient restClient, DoclingServeApiConfig config) {
        Assert.notNull(restClient, "restClient cannot be null");
        Assert.notNull(config, "config cannot be null");
        Assert.notNull(config.asyncExecutor(), "asyncExecutor cannot be null");
        this.restClient = restClient;
        this.config = config;
    }

    // CONFIG

    @Override
    public DoclingServeApiConfig config() {
        return config;
    }

    private Executor asyncExecutor() {
        Executor asyncExecutor = config.asyncExecutor();
        Assert.notNull(asyncExecutor, "asyncExecutor cannot be null");
        return asyncExecutor;
    }

    // HEALTH

    @Override
    public HealthCheckResponse health() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .requiredBody(HealthCheckResponse.class);
    }

    // CONVERT

    @Override
    public ConvertDocumentResponse convertSource(ConvertDocumentRequest request) {
        return restClient.post()
                .uri("/v1/convert/source")
                .body(request)
                .retrieve()
                .requiredBody(ConvertDocumentResponse.class);
    }

    @Override
    public CompletionStage<ConvertDocumentResponse> convertSourceAsync(ConvertDocumentRequest request) {
        return executeAsync("/v1/convert/source/async", request, this::convertTaskResult);
    }

    @Override
    public TaskStatusPollResponse convertSourceBatch(BatchConvertDocumentRequest request) {
        return restClient.post()
                .uri("/v1/convert/source/batch")
                .body(request)
                .retrieve()
                .requiredBody(TaskStatusPollResponse.class);
    }

    @Override
    public CompletionStage<ConvertDocumentResponse> convertSourceBatchAsync(BatchConvertDocumentRequest request) {
        return executeAsync("/v1/convert/source/batch", request, this::convertTaskResult);
    }

    // CHUNK

    @Override
    public ChunkDocumentResponse chunkSourceWithHierarchicalChunker(HierarchicalChunkDocumentRequest request) {
        return restClient.post()
                .uri("/v1/chunk/hierarchical/source")
                .body(request)
                .retrieve()
                .requiredBody(ChunkDocumentResponse.class);
    }

    @Override
    public ChunkDocumentResponse chunkSourceWithHybridChunker(HybridChunkDocumentRequest request) {
        return restClient.post()
                .uri("/v1/chunk/hybrid/source")
                .body(request)
                .retrieve()
                .requiredBody(ChunkDocumentResponse.class);
    }

    @Override
    public CompletionStage<ChunkDocumentResponse> chunkSourceWithHierarchicalChunkerAsync(HierarchicalChunkDocumentRequest request) {
        return executeAsync("/v1/chunk/hierarchical/source/async", request, this::chunkTaskResult);
    }

    @Override
    public CompletionStage<ChunkDocumentResponse> chunkSourceWithHybridChunkerAsync(HybridChunkDocumentRequest request) {
        return executeAsync("/v1/chunk/hybrid/source/async", request, this::chunkTaskResult);
    }

    // CLEAR

    @Override
    public ClearResponse clearConverters(ClearConvertersRequest request) {
        return restClient.get()
                .uri("/v1/clear/converters")
                .retrieve()
                .requiredBody(ClearResponse.class);
    }

    @Override
    public ClearResponse clearResults(ClearResultsRequest request) {
        return restClient.get()
                .uri("/v1/clear/results", uri -> uri.queryParam("older_then", request.getOlderThen().toSeconds()).build())
                .retrieve()
                .requiredBody(ClearResponse.class);
    }

    // TASK

    @Override
    public TaskStatusPollResponse pollTaskStatus(TaskStatusPollRequest request) {
        return restClient.get()
                .uri("/v1/status/poll/{taskId}", uriBuilder -> uriBuilder
                        .queryParam("wait", request.getWaitTime().toSeconds())
                        .build(request.getTaskId()))
                .retrieve()
                .requiredBody(TaskStatusPollResponse.class);
    }

    @Override
    public ConvertDocumentResponse convertTaskResult(TaskResultRequest request) {
        return restClient.get()
                .uri("/v1/result/{taskId}", uriBuilder -> uriBuilder
                        .build(request.getTaskId()))
                .retrieve()
                .requiredBody(ConvertDocumentResponse.class);
    }

    @Override
    public ChunkDocumentResponse chunkTaskResult(TaskResultRequest request) {
        return restClient.get()
                .uri("/v1/result/{taskId}", uriBuilder -> uriBuilder
                        .build(request.getTaskId()))
                .retrieve()
                .requiredBody(ChunkDocumentResponse.class);
    }

    // ASYNC

    /**
     * Submits a task to Docling Serve, polls its status until it completes, and then fetches its result.
     * The polling blocks the executor's thread, which is cheap with the default virtual threads.
     */
    private <T> CompletionStage<T> executeAsync(String uri, Object request, Function<TaskResultRequest, T> taskResult) {
        return CompletableFuture.supplyAsync(() -> {
            long deadline = System.nanoTime() + config.asyncTimeout().toNanos();
            TaskStatusPollResponse task = restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .requiredBody(TaskStatusPollResponse.class);

            while (task.getTaskStatus() == TaskStatus.PENDING || task.getTaskStatus() == TaskStatus.STARTED) {
                if (System.nanoTime() - deadline > 0) {
                    throw new CompletionException(new TimeoutException(
                            "Docling Serve task %s did not complete within %s".formatted(task.getTaskId(), config.asyncTimeout())));
                }
                sleep(config.asyncPollInterval());
                task = pollTaskStatus(TaskStatusPollRequest.builder().taskId(task.getTaskId()).build());
            }

            if (task.getTaskStatus() == TaskStatus.FAILURE) {
                throw new IllegalStateException("Docling Serve task %s failed".formatted(task.getTaskId()));
            }
            return taskResult.apply(TaskResultRequest.builder().taskId(task.getTaskId()).build());
        }, asyncExecutor());
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CompletionException(ex);
        }
    }

    // BUILDER

    /**
     * Not supported. Use {@code config().toBuilder()} to create a modified copy of this client.
     */
    @Override
    @SuppressWarnings({"removal"})
    public <T extends DoclingServeApi, B extends DoclingApiBuilder<T, B>> DoclingApiBuilder<T, B> toBuilder() {
        throw new UnsupportedOperationException("toBuilder is not supported. Use config().toBuilder() instead.");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private RestClient.Builder restClientBuilder = RestClient.builder();
        private HttpClientSettings httpClientSettings = HttpClientSettings.defaults();
        @Nullable
        private URI baseUrl;
        @Nullable
        private String apiKey;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration asyncPollInterval = Duration.ofSeconds(2);
        private Duration asyncTimeout = Duration.ofMinutes(5);
        private Executor asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        private boolean logRequests = false;
        private boolean logResponses = false;
        private boolean prettyPrint = false;

        private Builder() {}

        public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            this.restClientBuilder = restClientBuilder;
            return this;
        }

        /**
         * Settings for the underlying HTTP client, such as the SSL bundle and the redirect strategy.
         * The connect and read timeouts of this builder take precedence over the ones in the settings.
         */
        public Builder httpClientSettings(HttpClientSettings httpClientSettings) {
            this.httpClientSettings = httpClientSettings;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            Assert.hasText(baseUrl, "baseUrl cannot be null or empty");
            return baseUrl(URI.create(baseUrl));
        }

        public Builder baseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(@Nullable String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder asyncPollInterval(Duration asyncPollInterval) {
            this.asyncPollInterval = asyncPollInterval;
            return this;
        }

        public Builder asyncTimeout(Duration asyncTimeout) {
            this.asyncTimeout = asyncTimeout;
            return this;
        }

        public Builder asyncExecutor(Executor asyncExecutor) {
            this.asyncExecutor = asyncExecutor;
            return this;
        }

        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public Builder prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        public DoclingServeClient build() {
            Assert.notNull(restClientBuilder, "restClientBuilder cannot be null");
            Assert.notNull(httpClientSettings, "httpClientSettings cannot be null");
            URI baseUrl = this.baseUrl;
            Assert.notNull(baseUrl, "baseUrl cannot be null");
            boolean plainHttp = "http".equalsIgnoreCase(baseUrl.getScheme());
            Assert.isTrue(plainHttp || "https".equalsIgnoreCase(baseUrl.getScheme()), "baseUrl must be an http or https URL");
            Assert.notNull(connectTimeout, "connectTimeout cannot be null");
            Assert.notNull(readTimeout, "readTimeout cannot be null");
            Assert.notNull(asyncPollInterval, "asyncPollInterval cannot be null");
            Assert.notNull(asyncTimeout, "asyncTimeout cannot be null");
            Assert.notNull(asyncExecutor, "asyncExecutor cannot be null");
            Assert.isTrue(connectTimeout.isPositive(), "connectTimeout must be positive");
            Assert.isTrue(readTimeout.isPositive(), "readTimeout must be positive");
            Assert.isTrue(asyncPollInterval.isPositive(), "asyncPollInterval must be positive");
            Assert.isTrue(asyncTimeout.isPositive(), "asyncTimeout must be positive");

            restClientBuilder
                    .baseUrl(baseUrl)
                    .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                            .withHttpClientCustomizer(builder -> {
                                if (plainHttp) {
                                    // Docling Serve uses Python FastAPI which causes errors when called from JDK HttpClient.
                                    // The HttpClient uses HTTP 2 by default and then falls back to HTTP 1.1 if not supported.
                                    // However, the way FastAPI works results in the fallback not happening, making the call fail.
                                    builder.version(HttpClient.Version.HTTP_1_1);
                                }
                            })
                            .build(httpClientSettings.withTimeouts(connectTimeout, readTimeout)));

            if (StringUtils.hasText(apiKey)) {
                restClientBuilder.defaultHeader(DoclingProperties.API_KEY_HEADER_NAME, apiKey);
            }

            restClientBuilder.defaultStatusHandler(ValidationErrorHandler.STATUS::equals, new ValidationErrorHandler());

            if (logRequests || logResponses) {
                restClientBuilder.requestInterceptor(new LoggingClientHttpRequestInterceptor(logRequests, logResponses, prettyPrint));
            }
            if (logResponses) {
                // Buffer the response bodies so that they can be logged and then still read by the response handlers.
                restClientBuilder.bufferContent((uri, method) -> true);
            }

            DoclingServeApiConfig config = DoclingServeApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .asyncPollInterval(asyncPollInterval)
                    .asyncTimeout(asyncTimeout)
                    .asyncExecutor(asyncExecutor)
                    .logRequests(logRequests)
                    .logResponses(logResponses)
                    .prettyPrint(prettyPrint)
                    .config();

            return new DoclingServeClient(restClientBuilder.build(), config);
        }

    }

}
