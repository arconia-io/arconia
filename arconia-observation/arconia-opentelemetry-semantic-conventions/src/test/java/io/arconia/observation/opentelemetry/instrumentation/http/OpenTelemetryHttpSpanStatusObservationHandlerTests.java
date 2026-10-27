package io.arconia.observation.opentelemetry.instrumentation.http;

import java.io.IOException;
import java.net.URI;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryHttpSpanStatusObservationHandler}.
 * <p>
 * Spans not in error are asserted as not {@code Error} rather than unset, because the Micrometer Tracing
 * OpenTelemetry bridge sets the {@code Ok} status on the spans created by the default tracing handler, like the ones
 * in these tests, when they end without an error.
 */
class OpenTelemetryHttpSpanStatusObservationHandlerTests {

    private final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

    private final ObservationRegistry registry = ObservationRegistry.create();

    OpenTelemetryHttpSpanStatusObservationHandlerTests() {
        OtelTracer tracer = new OtelTracer(tracerProvider.get("test"), new OtelCurrentTraceContext(), event -> {});
        // Registered after the tracing handler, as Spring Boot does, so that it runs before the span ends.
        registry.observationConfig()
                .observationHandler(new DefaultTracingObservationHandler(tracer))
                .observationHandler(new OpenTelemetryHttpSpanStatusObservationHandler());
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    // SERVER

    @Test
    void serverSpanIsErrorForServerErrorStatus() {
        assertThat(observe(serverContext(503, null)).getStatusCode()).isEqualTo(StatusCode.ERROR);
    }

    @Test
    void serverSpanIsUnsetForClientErrorStatus() {
        assertThat(observe(serverContext(404, null)).getStatusCode()).isNotEqualTo(StatusCode.ERROR);
    }

    @Test
    void serverSpanIsUnsetForSuccessStatus() {
        assertThat(observe(serverContext(200, null)).getStatusCode()).isNotEqualTo(StatusCode.ERROR);
    }

    @Test
    void serverSpanKeepsErrorFromException() {
        StatusData status = observe(serverContext(500, new IllegalStateException("boom")));

        assertThat(status.getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(status.getDescription()).isEqualTo("boom");
    }

    // CLIENT

    @Test
    void clientSpanIsErrorForClientErrorStatus() {
        assertThat(observe(clientContext(HttpStatus.NOT_FOUND)).getStatusCode()).isEqualTo(StatusCode.ERROR);
    }

    @Test
    void clientSpanIsErrorForServerErrorStatus() {
        assertThat(observe(clientContext(HttpStatus.SERVICE_UNAVAILABLE)).getStatusCode()).isEqualTo(StatusCode.ERROR);
    }

    @Test
    void clientSpanIsUnsetForRedirectStatus() {
        assertThat(observe(clientContext(HttpStatus.FOUND)).getStatusCode()).isNotEqualTo(StatusCode.ERROR);
    }

    @Test
    void clientSpanIsUnsetWithoutResponse() {
        ClientRequestObservationContext context = new ClientRequestObservationContext(
                new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com")));

        assertThat(observe(context).getStatusCode()).isNotEqualTo(StatusCode.ERROR);
    }

    @Test
    void clientSpanKeepsErrorFromException() {
        ClientRequestObservationContext context = new ClientRequestObservationContext(
                new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com")));
        context.setError(new IOException("connection refused"));

        StatusData status = observe(context);

        assertThat(status.getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(status.getDescription()).isEqualTo("connection refused");
    }

    // SUPPORT

    @Test
    void supportsOnlyHttpContexts() {
        var handler = new OpenTelemetryHttpSpanStatusObservationHandler();

        assertThat(handler.supportsContext(serverContext(200, null))).isTrue();
        assertThat(handler.supportsContext(clientContext(HttpStatus.OK))).isTrue();
        assertThat(handler.supportsContext(new Observation.Context())).isFalse();
    }

    private StatusData observe(Observation.Context context) {
        Observation observation = Observation.createNotStarted("http", () -> context, registry).start();
        if (context.getError() != null) {
            observation.error(context.getError());
        }
        observation.stop();

        assertThat(spanExporter.getFinishedSpanItems()).hasSize(1);
        return spanExporter.getFinishedSpanItems().getFirst().getStatus();
    }

    private static ServerRequestObservationContext serverContext(int status, Throwable error) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        ServerRequestObservationContext context = new ServerRequestObservationContext(new MockHttpServletRequest(), response);
        if (error != null) {
            context.setError(error);
        }
        return context;
    }

    private static ClientRequestObservationContext clientContext(HttpStatus status) {
        ClientRequestObservationContext context = new ClientRequestObservationContext(
                new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com")));
        context.setResponse(new MockClientHttpResponse(new byte[0], status));
        return context;
    }

}
