package io.arconia.observation.opentelemetry.instrumentation.http;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Unit tests for {@link OpenTelemetryClientRequestObservationConvention}.
 */
class OpenTelemetryClientRequestObservationConventionTests {

    private final OpenTelemetryClientRequestObservationConvention convention =
            new OpenTelemetryClientRequestObservationConvention(new OpenTelemetryHttpOptions());

    // NAMES

    @Test
    void nameIsClientRequestDuration() {
        assertThat(convention.getName()).isEqualTo("http.client.request.duration");
    }

    @Test
    void contextualNameIsMethod() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com/users/42");

        assertThat(convention.getContextualName(context)).isEqualTo("GET");
    }

    @Test
    void contextualNameIsHttpForUnknownMethod() {
        ClientRequestObservationContext context = context(HttpMethod.valueOf("CUSTOM"), "https://example.com");

        assertThat(convention.getContextualName(context)).isEqualTo("HTTP");
    }

    // LOW CARDINALITY

    @Test
    void lowCardinalityKeyValuesForSuccessfulRequest() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com/users/42");
        context.setUriTemplate("https://example.com/users/{id}");
        context.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).containsExactly(
                KeyValue.of("error.type", KeyValue.NONE_VALUE),
                KeyValue.of("http.request.method", "GET"),
                KeyValue.of("http.response.status_code", "200"),
                KeyValue.of("outcome", "SUCCESS"),
                KeyValue.of("server.address", "example.com"),
                KeyValue.of("server.port", "443"),
                KeyValue.of("uri", "/users/{id}"));
    }

    @Test
    void connectIsKnownMethod() {
        ClientRequestObservationContext context = context(HttpMethod.valueOf("CONNECT"), "https://example.com");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("http.request.method", "CONNECT"));
        assertThat(convention.getHighCardinalityKeyValues(context)).noneMatch(kv -> "http.request.method_original".equals(kv.getKey()));
    }

    @Test
    void unknownMethodIsOther() {
        ClientRequestObservationContext context = context(HttpMethod.valueOf("CUSTOM"), "https://example.com");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("http.request.method", "_OTHER"));
        assertThat(convention.getHighCardinalityKeyValues(context)).contains(KeyValue.of("http.request.method_original", "CUSTOM"));
    }

    @Test
    void statusCodeAbsentWithoutResponse() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com");

        assertThat(convention.getLowCardinalityKeyValues(context))
                .noneMatch(kv -> "http.response.status_code".equals(kv.getKey()));
    }

    @Test
    void statusCodeAbsentWhenResponseFailsToProvideIt() throws IOException {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com");
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        given(response.getStatusCode()).willThrow(new IOException("boom"));
        context.setResponse(response);

        assertThat(convention.getLowCardinalityKeyValues(context))
                .noneMatch(kv -> "http.response.status_code".equals(kv.getKey()))
                .contains(KeyValue.of("error.type", KeyValue.NONE_VALUE));
    }

    @Test
    void errorTypeIsStatusCodeForErrorResponse() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com");
        context.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.NOT_FOUND));
        context.setError(new IllegalStateException("boom"));

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("error.type", "404"));
    }

    @Test
    void errorTypeIsExceptionTypeWithoutResponse() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com");
        context.setError(new IOException("connection refused"));

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("error.type", "java.io.IOException"));
    }

    @Test
    void errorTypeIsCauseOfResourceAccessException() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com");
        context.setError(new ResourceAccessException("I/O error", new UnknownHostException("example.com")));

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("error.type", "java.net.UnknownHostException"));
    }

    @Test
    void errorTypeIsNoneForRedirect() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com");
        context.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.FOUND));

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("error.type", KeyValue.NONE_VALUE));
    }

    @Test
    void serverPortIsExplicitPort() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "http://localhost:8080/api");

        assertThat(convention.getLowCardinalityKeyValues(context))
                .contains(KeyValue.of("server.address", "localhost"), KeyValue.of("server.port", "8080"));
    }

    @Test
    void serverPortIsDefaultPortOfHttpScheme() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "http://example.com/api");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("server.port", "80"));
    }

    @Test
    void uriIsNoneWithoutUriTemplate() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com/users/42");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("uri", KeyValue.NONE_VALUE));
    }

    // HIGH CARDINALITY

    @Test
    void urlFullIsRequestUrl() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com/search?q=OpenTelemetry#results");

        assertThat(convention.getHighCardinalityKeyValues(context))
                .containsExactly(KeyValue.of("url.full", "https://example.com/search?q=OpenTelemetry#results"));
    }

    @Test
    void urlFullRedactsCredentials() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://user:secret@example.com:8443/path");

        assertThat(convention.getHighCardinalityKeyValues(context))
                .containsExactly(KeyValue.of("url.full", "https://REDACTED:REDACTED@example.com:8443/path"));
    }

    @Test
    void urlFullRedactsSensitiveQueryParameters() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com/file?sig=secret&page=2");

        assertThat(convention.getHighCardinalityKeyValues(context))
                .containsExactly(KeyValue.of("url.full", "https://example.com/file?sig=REDACTED&page=2"));
    }

    @Test
    void urlFullUsesConfiguredSensitiveQueryParameters() {
        OpenTelemetryHttpOptions options = new OpenTelemetryHttpOptions();
        options.setSensitiveQueryParameters(List.of("token"));
        var customConvention = new OpenTelemetryClientRequestObservationConvention(options);
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com/file?token=secret&sig=other");

        assertThat(customConvention.getHighCardinalityKeyValues(context))
                .containsExactly(KeyValue.of("url.full", "https://example.com/file?token=REDACTED&sig=other"));
    }

    @Test
    void urlFullKeepsEncoding() {
        ClientRequestObservationContext context = context(HttpMethod.GET, "https://example.com/a%20b?q=a%26b&sig=x%3Dy");

        assertThat(convention.getHighCardinalityKeyValues(context))
                .containsExactly(KeyValue.of("url.full", "https://example.com/a%20b?q=a%26b&sig=REDACTED"));
    }

    // REST CLIENT

    @Test
    void appliesToRestClientRequestEndingWithErrorStatus() {
        Observation.Context context = observeRestClientCall(withStatus(HttpStatus.SERVICE_UNAVAILABLE),
                HttpServerErrorException.class);

        assertThat(context.getName()).isEqualTo("http.client.request.duration");
        assertThat(context.getContextualName()).isEqualTo("GET");
        assertThat(context.getLowCardinalityKeyValues()).contains(
                KeyValue.of("error.type", "503"),
                KeyValue.of("http.request.method", "GET"),
                KeyValue.of("http.response.status_code", "503"),
                KeyValue.of("uri", "/users/{id}"));
        assertThat(context.getHighCardinalityKeyValues()).contains(KeyValue.of("url.full", "https://example.com/users/42"));
    }

    @Test
    void appliesToRestClientRequestFailingWithIoError() {
        Observation.Context context = observeRestClientCall(withException(new ConnectException("Connection refused")),
                ResourceAccessException.class);

        assertThat(context.getLowCardinalityKeyValues()).contains(KeyValue.of("error.type", "java.net.ConnectException"));
        assertThat(context.getLowCardinalityKeyValues()).noneMatch(kv -> "http.response.status_code".equals(kv.getKey()));
    }

    private Observation.Context observeRestClientCall(ResponseCreator response, Class<? extends Exception> expectedException) {
        AtomicReference<Observation.Context> observed = new AtomicReference<>();
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(new ObservationHandler<>() {
            @Override
            public void onStop(Observation.Context context) {
                observed.set(context);
            }

            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        });
        RestClient.Builder builder = RestClient.builder().observationRegistry(registry).observationConvention(convention);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://example.com/users/42")).andRespond(response);
        RestClient restClient = builder.build();

        assertThatThrownBy(() -> restClient.get().uri("https://example.com/users/{id}", 42).retrieve().toBodilessEntity())
                .isInstanceOf(expectedException);
        return observed.get();
    }

    private static ClientRequestObservationContext context(HttpMethod method, String url) {
        return new ClientRequestObservationContext(new MockClientHttpRequest(method, URI.create(url)));
    }

}
