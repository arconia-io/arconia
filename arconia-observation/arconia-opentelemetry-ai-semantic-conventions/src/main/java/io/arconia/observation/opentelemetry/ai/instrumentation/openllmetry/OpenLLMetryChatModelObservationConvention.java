package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.observation.ChatModelObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;

/**
 * OpenLLMetry flavor of {@link OpenTelemetryChatModelObservationConvention}.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryChatModelObservationConvention extends OpenTelemetryChatModelObservationConvention {

    public OpenLLMetryChatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    // HIGH CARDINALITY

    @Override
    protected KeyValues requestStream(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.isStreaming()) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_IS_STREAMING, String.valueOf(true));
        }
        return keyValues;
    }

    @Override
    protected boolean recordsContentOnSpanAttributes() {
        // OpenLLMetry records content on span attributes only.
        return true;
    }

}
