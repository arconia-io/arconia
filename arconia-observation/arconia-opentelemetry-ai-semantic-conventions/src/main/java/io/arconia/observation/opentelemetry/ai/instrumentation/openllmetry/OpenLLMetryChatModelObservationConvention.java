package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.observation.ChatModelObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * OpenLLMetry flavor of {@link OpenTelemetryChatModelObservationConvention}.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryChatModelObservationConvention extends OpenTelemetryChatModelObservationConvention {

    public OpenLLMetryChatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    // LOW CARDINALITY

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM,
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    // HIGH CARDINALITY

    @Override
    protected KeyValues requestStream(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.isStreaming()) {
            keyValues = keyValues.and(OpenLLMetryAttributes.GEN_AI_IS_STREAMING, String.valueOf(true));
        }
        return keyValues;
    }

}
