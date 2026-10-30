package io.arconia.observation.opentelemetry.ai.instrumentation.langfuse;

import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes;
import io.micrometer.common.KeyValues;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConversations;

/**
 * Langfuse flavor of {@link OpenTelemetryChatClientObservationConvention}.
 *
 * @see <a href="https://langfuse.com/integrations/native/opentelemetry">Langfuse OpenTelemetry</a>
 */
public final class LangfuseChatClientObservationConvention extends OpenTelemetryChatClientObservationConvention {

    public LangfuseChatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    /**
     * Records the conversation also as {@code session.id}: Langfuse reads {@code gen_ai.conversation.id} only
     * from the root span of a trace, while {@code session.id} sets the session from any span, such as a chat
     * client call nested in an HTTP request.
     */
    @Override
    protected KeyValues conversationId(KeyValues keyValues, ChatClientObservationContext context) {
        String conversationId = GenAiConversations.conversationId(context);
        return conversationId != null
                ? super.conversationId(keyValues, context).and(SessionIncubatingAttributes.SESSION_ID.getKey(), conversationId)
                : keyValues;
    }

    @Override
    protected boolean recordsContentOnSpanAttributes() {
        return true;
    }

}
