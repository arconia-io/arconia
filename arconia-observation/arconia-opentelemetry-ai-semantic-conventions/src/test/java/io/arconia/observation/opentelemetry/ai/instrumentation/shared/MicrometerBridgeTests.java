package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelSpan;
import io.opentelemetry.api.trace.Span;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerBridge}.
 */
class MicrometerBridgeTests {

    @Test
    void shouldReturnNullWithoutTracingContext() {
        assertThat(MicrometerBridge.extractOtelSpan(null)).isNull();
    }

    @Test
    void shouldReturnNullWithoutSpan() {
        assertThat(MicrometerBridge.extractOtelSpan(new TracingObservationHandler.TracingContext())).isNull();
    }

    @Test
    void shouldReturnTheOpenTelemetrySpanBehindTheMicrometerOne() {
        Span span = Span.getInvalid();
        TracingObservationHandler.TracingContext tracingContext = new TracingObservationHandler.TracingContext();
        tracingContext.setSpan(OtelSpan.fromOtel(span));

        assertThat(MicrometerBridge.extractOtelSpan(tracingContext)).isSameAs(span);
    }

}
