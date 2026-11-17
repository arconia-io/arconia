package io.arconia.docling.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

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
    void whenBaseUrlIsNotHttpThenThrow() {
        assertThatThrownBy(() -> DoclingServeClient.builder().baseUrl("localhost:5001").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl must be an http or https URL");

        assertThatThrownBy(() -> DoclingServeClient.builder().baseUrl("/docling").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl must be an http or https URL");
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

        assertThatThrownBy(() -> DoclingServeClient.builder()
                .baseUrl("http://localhost:5001")
                .asyncPollInterval(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asyncPollInterval must be positive");

        assertThatThrownBy(() -> DoclingServeClient.builder()
                .baseUrl("http://localhost:5001")
                .asyncTimeout(Duration.ofSeconds(-1))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asyncTimeout must be positive");
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
        assertThat(config.bearerToken()).isNull();
        assertThat(config.isExplicitlySet(DoclingServeApiConfig.BEARER_TOKEN)).isFalse();
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.readTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(config.asyncPollInterval()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.asyncTimeout()).isEqualTo(Duration.ofMinutes(5));
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
                .bearerToken("token")
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .asyncPollInterval(Duration.ofSeconds(1))
                .asyncTimeout(Duration.ofMinutes(10))
                .build();

        DoclingServeApiConfig expected = DoclingServeApi.builder()
                .baseUrl("https://docling.example.com")
                .apiKey("secret")
                .bearerToken("token")
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .asyncPollInterval(Duration.ofSeconds(1))
                .asyncTimeout(Duration.ofMinutes(10))
                .asyncExecutor(Objects.requireNonNull(client.config().asyncExecutor()))
                .logRequests(false)
                .logResponses(false)
                .prettyPrint(false)
                .config();

        assertThat(client.config()).isEqualTo(expected);
        assertThat(client.config().toBuilder().config()).isEqualTo(expected);
    }

    @Test
    void sendsTheCredentialsInEveryRequest() {
        AtomicReference<HttpHeaders> sentHeaders = new AtomicReference<>();
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    sentHeaders.set(request.getHeaders());
                    return jsonResponse("{\"status\":\"ok\"}");
                });

        DoclingServeClient.builder()
                .restClientBuilder(restClientBuilder)
                .baseUrl("http://localhost:5001")
                .apiKey("secret")
                .bearerToken("token")
                .build()
                .health();

        assertThat(sentHeaders.get().getFirst(DoclingServeClient.API_KEY_HEADER_NAME)).isEqualTo("secret");
        assertThat(sentHeaders.get().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    }

    @Test
    void whenAsyncTaskFailsThenReportTheErrorMessage() {
        DoclingServeClient client = clientRespondingWith("""
                {"task_id": "task-1", "task_type": "convert", "task_status": "failure", "error_message": "Out of memory"}
                """).build();

        assertThat(client.convertSourceAsync(ConvertDocumentRequest.builder().build()).toCompletableFuture())
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableThat()
                .havingCause()
                .isInstanceOf(IllegalStateException.class)
                .withMessage("Docling Serve task task-1 failed: Out of memory");
    }

    @Test
    void whenAsyncTaskFailsWithoutErrorMessageThenReportTheTask() {
        DoclingServeClient client = clientRespondingWith("""
                {"task_id": "task-1", "task_type": "convert", "task_status": "failure"}
                """).build();

        assertThat(client.convertSourceAsync(ConvertDocumentRequest.builder().build()).toCompletableFuture())
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableThat()
                .havingCause()
                .isInstanceOf(IllegalStateException.class)
                .withMessage("Docling Serve task task-1 failed");
    }

    @Test
    void whenAsyncTaskDoesNotCompleteInTimeThenTimeout() {
        DoclingServeClient client = clientRespondingWith("""
                {"task_id": "task-1", "task_type": "convert", "task_status": "started"}
                """)
                .asyncPollInterval(Duration.ofMillis(10))
                .asyncTimeout(Duration.ofMillis(100))
                .build();

        assertThat(client.convertSourceAsync(ConvertDocumentRequest.builder().build()).toCompletableFuture())
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableThat()
                .havingCause()
                .isInstanceOf(TimeoutException.class)
                .withMessage("Docling Serve task task-1 did not complete within PT0.1S");
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

    /**
     * The client replaces the request factory when it's built, so an interceptor answers the requests instead.
     */
    private static DoclingServeClient.Builder clientRespondingWith(String json) {
        return DoclingServeClient.builder()
                .restClientBuilder(RestClient.builder()
                        .requestInterceptor((request, body, execution) -> jsonResponse(json)))
                .baseUrl("http://localhost:5001");
    }

    private static MockClientHttpResponse jsonResponse(String json) {
        MockClientHttpResponse response = new MockClientHttpResponse(json.getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response;
    }

}
