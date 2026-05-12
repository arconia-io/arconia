package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * OpenLIT flavor of {@link OpenTelemetryChatClientObservationConvention}.
 *
 * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
 */
public final class OpenLitChatClientObservationConvention extends OpenTelemetryChatClientObservationConvention {

    public OpenLitChatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    @Override
    protected KeyValue aiProvider(ChatClientObservationContext context) {
        return KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM,
                GenAiConventionsConverter.toOpenLitProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    protected KeyValue stream(ChatClientObservationContext context) {
        return KeyValue.of(OpenLitAttributes.GEN_AI_REQUEST_IS_STREAM, String.valueOf(context.isStream()));
    }

}
