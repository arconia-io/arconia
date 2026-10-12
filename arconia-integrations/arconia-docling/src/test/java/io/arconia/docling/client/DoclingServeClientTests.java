package io.arconia.docling.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DoclingServeClient}.
 */
class DoclingServeClientTests {

    @Test
    void whenBaseUrlIsMissingThenThrow() {
        assertThatThrownBy(() -> DoclingServeClient.builder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl cannot be null");
    }

    @Test
    void whenBaseUrlStringIsBlankThenThrow() {
        assertThatThrownBy(() -> DoclingServeClient.builder().baseUrl(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl cannot be null or empty");
    }

    @Test
    void whenTimeoutIsNotPositiveThenThrow() {
        assertThatThrownBy(() -> DoclingServeClient.builder()
                .baseUrl("http://localhost:5001")
                .connectTimeout(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectTimeout must be positive");

        assertThatThrownBy(() -> DoclingServeClient.builder()
                .baseUrl("http://localhost:5001")
                .readTimeout(Duration.ofSeconds(-1))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readTimeout must be positive");
    }

    @Test
    void configReportsDefaults() {
        DoclingServeClient client = DoclingServeClient.builder()
                .baseUrl("http://localhost:5001")
                .build();

        DoclingServeApiConfig config = client.config();
        assertThat(config.baseUrl()).isEqualTo(URI.create("http://localhost:5001"));
        assertThat(config.apiKey()).isNull();
        assertThat(config.isExplicitlySet(DoclingServeApiConfig.API_KEY)).isFalse();
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.readTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.asyncExecutor()).isNotNull();
        assertThat(config.logRequests()).isFalse();
        assertThat(config.logResponses()).isFalse();
        assertThat(config.prettyPrint()).isFalse();
    }

    @Test
    void configReportsLoggingOptions() {
        DoclingServeClient client = DoclingServeClient.builder()
                .baseUrl("http://localhost:5001")
                .logRequests(true)
                .logResponses(true)
                .prettyPrint(true)
                .build();

        assertThat(client.config().logRequests()).isTrue();
        assertThat(client.config().logResponses()).isTrue();
        assertThat(client.config().prettyPrint()).isTrue();
    }

    @Test
    void configReportsAsyncExecutorWhenSet() {
        Executor asyncExecutor = Executors.newSingleThreadExecutor();
        DoclingServeClient client = DoclingServeClient.builder()
                .baseUrl("http://localhost:5001")
                .asyncExecutor(asyncExecutor)
                .build();

        assertThat(client.config().asyncExecutor()).isSameAs(asyncExecutor);
    }

    @Test
    void configReportsTheEffectiveSettings() {
        DoclingServeClient client = DoclingServeClient.builder()
                .baseUrl(URI.create("https://docling.example.com"))
                .apiKey("secret")
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();

        DoclingServeApiConfig expected = DoclingServeApi.builder()
                .baseUrl("https://docling.example.com")
                .apiKey("secret")
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .asyncExecutor(Objects.requireNonNull(client.config().asyncExecutor()))
                .logRequests(false)
                .logResponses(false)
                .prettyPrint(false)
                .config();

        assertThat(client.config()).isEqualTo(expected);
        assertThat(client.config().toBuilder().config()).isEqualTo(expected);
    }

    @Test
    void toBuilderIsNotSupported() {
        DoclingServeClient client = DoclingServeClient.builder()
                .baseUrl("http://localhost:5001")
                .build();

        assertThatThrownBy(client::toBuilder)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("config().toBuilder()");
    }

}
