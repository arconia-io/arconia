package io.arconia.docling.client;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;
import ai.docling.serve.api.spi.DoclingServeApiProvider.Unsupported;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DoclingServeClientProvider}.
 */
class DoclingServeClientProviderTests {

    @Test
    void providerIsDiscovered() {
        DoclingServeApi api = DoclingServeApi.builder()
                .baseUrl("http://localhost:5001")
                .build();

        assertThat(api).isInstanceOf(DoclingServeClient.class);
    }

    @Test
    void configReportsTheEffectiveSettings() {
        Executor asyncExecutor = Executors.newSingleThreadExecutor();
        DoclingServeApi api = DoclingServeApi.builder()
                .baseUrl("http://localhost:5001")
                .apiKey("key")
                .readTimeout(Duration.ofSeconds(10))
                .asyncExecutor(asyncExecutor)
                .logRequests()
                .prettyPrint()
                .build();

        DoclingServeApiConfig config = api.config();
        assertThat(config.logRequests()).isTrue();
        assertThat(config.logResponses()).isFalse();
        assertThat(config.prettyPrint()).isTrue();
        assertThat(config.baseUrl()).isEqualTo(URI.create("http://localhost:5001"));
        assertThat(config.apiKey()).isEqualTo("key");
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.readTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.asyncExecutor()).isSameAs(asyncExecutor);

        assertThat(config.toBuilder().build().config()).isEqualTo(config);
    }

    @Test
    void unsupportedOptionsAreDeclared() {
        DoclingServeClientProvider provider = new DoclingServeClientProvider();

        assertThat(provider.unsupportedOptions()).containsOnlyKeys(
                DoclingServeApiConfig.ASYNC_POLL_INTERVAL,
                DoclingServeApiConfig.ASYNC_TIMEOUT);
        assertThat(provider.unsupportedOptions().values()).containsOnly(Unsupported.WARN);
    }

}
