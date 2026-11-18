package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.trace.Span;

import org.jspecify.annotations.Nullable;

public final class MicrometerBridge {

    private MicrometerBridge() {}

    /**
     * Returns the OpenTelemetry span behind the span of the given tracing context, or null when there is no span or
     * it doesn't come from the OpenTelemetry bridge, such as the spans of the no-op tracer that Spring Boot falls back
     * to when no OpenTelemetry tracer is configured.
     */
    @Nullable
    public static Span extractOtelSpan(TracingObservationHandler.@Nullable TracingContext tracingContext) {
        if (tracingContext == null || !(tracingContext.getSpan() instanceof OtelSpan otelSpan)) {
            return null;
        }
        return OtelSpan.toOtel(otelSpan);
    }

}
