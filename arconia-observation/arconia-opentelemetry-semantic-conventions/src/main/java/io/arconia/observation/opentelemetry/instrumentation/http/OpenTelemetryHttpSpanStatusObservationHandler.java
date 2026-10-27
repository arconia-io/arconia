package io.arconia.observation.opentelemetry.instrumentation.http;

import java.io.IOException;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.trace.StatusCode;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * {@link ObservationHandler} setting the status of HTTP spans to {@code Error} when the response status code
 * signals an error but no exception was recorded, as the OpenTelemetry Semantic Conventions require:
 * {@code 5xx} for server spans, {@code 4xx} and {@code 5xx} for client spans.
 * <p>
 * When an exception was recorded, the tracing handler already sets the {@code Error} status.
 * This handler must be registered after the tracing handler, so that it runs before the span ends.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/http-spans/#status">HTTP Span Status</a>
 */
public final class OpenTelemetryHttpSpanStatusObservationHandler implements ObservationHandler<Observation.Context> {

    @Override
    public void onStop(Observation.Context context) {
        if (context.getError() != null || !isErrorStatus(context)) {
            return;
        }
        TracingObservationHandler.TracingContext tracingContext = context.get(TracingObservationHandler.TracingContext.class);
        if (tracingContext != null && tracingContext.getSpan() != null) {
            OtelSpan.toOtel(tracingContext.getSpan()).setStatus(StatusCode.ERROR);
        }
    }

    private static boolean isErrorStatus(Observation.Context context) {
        if (context instanceof ServerRequestObservationContext serverContext && serverContext.getResponse() != null) {
            return serverContext.getResponse().getStatus() >= 500;
        }
        if (context instanceof ClientRequestObservationContext clientContext && clientContext.getResponse() != null) {
            return getStatusCode(clientContext.getResponse()) >= 400;
        }
        return false;
    }

    private static int getStatusCode(ClientHttpResponse response) {
        try {
            return response.getStatusCode().value();
        }
        catch (IOException ex) {
            return -1;
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ServerRequestObservationContext || context instanceof ClientRequestObservationContext;
    }

}
