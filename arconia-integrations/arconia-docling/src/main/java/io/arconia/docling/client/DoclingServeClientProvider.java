package io.arconia.docling.client;

import java.util.Map;
import java.util.concurrent.Executor;

import ai.docling.serve.api.ConfigOption;
import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;
import ai.docling.serve.api.spi.DoclingServeApiProvider;

/**
 * Provides a {@link DoclingServeClient} to {@link DoclingServeApi#builder()}, so that the standard
 * Docling Java entry point builds the Arconia client when this module is on the classpath.
 */
public final class DoclingServeClientProvider implements DoclingServeApiProvider {

    @Override
    public DoclingServeApi create(DoclingServeApiConfig config) {
        DoclingServeClient.Builder builder = DoclingServeClient.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .logRequests(config.logRequests())
                .logResponses(config.logResponses())
                .prettyPrint(config.prettyPrint());

        // Null when not set: keep the client's default (virtual threads) rather than passing null on.
        Executor asyncExecutor = config.asyncExecutor();
        if (asyncExecutor != null) {
            builder.asyncExecutor(asyncExecutor);
        }

        return builder.build();
    }

    @Override
    public Map<ConfigOption<?>, Unsupported> unsupportedOptions() {
        // The async operations don't poll for task status.
        return Map.of(
                DoclingServeApiConfig.ASYNC_POLL_INTERVAL, Unsupported.WARN,
                DoclingServeApiConfig.ASYNC_TIMEOUT, Unsupported.WARN
        );
    }

}
