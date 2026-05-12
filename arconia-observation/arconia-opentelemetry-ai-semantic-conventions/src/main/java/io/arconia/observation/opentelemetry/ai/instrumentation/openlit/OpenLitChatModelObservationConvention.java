package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.observation.ChatModelObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * OpenLIT flavor of {@link OpenTelemetryChatModelObservationConvention}.
 *
 * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
 */
public final class OpenLitChatModelObservationConvention extends OpenTelemetryChatModelObservationConvention {

    public OpenLitChatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM,
                GenAiConventionsConverter.toOpenLitProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    protected KeyValues requestStream(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.isStreaming()) {
            return keyValues.and(OpenLitAttributes.GEN_AI_REQUEST_IS_STREAM, String.valueOf(true));
        }
        return keyValues;
    }

}
