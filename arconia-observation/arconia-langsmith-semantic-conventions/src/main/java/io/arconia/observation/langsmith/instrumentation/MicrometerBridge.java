package io.arconia.observation.langsmith.instrumentation;

import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.trace.Span;

import org.jspecify.annotations.Nullable;

final class MicrometerBridge {

    private MicrometerBridge() {}

    @Nullable
    public static Span extractOtelSpan(TracingObservationHandler.@Nullable TracingContext tracingContext) {
        if (tracingContext == null || tracingContext.getSpan() == null) {
            return null;
        }
        return OtelSpan.toOtel(tracingContext.getSpan());
    }

}
