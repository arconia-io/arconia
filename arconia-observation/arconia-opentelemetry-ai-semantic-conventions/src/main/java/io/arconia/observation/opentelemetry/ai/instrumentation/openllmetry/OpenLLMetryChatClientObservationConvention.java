package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.CollectionUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConversations;

/**
 * OpenLLMetry flavor of {@link OpenTelemetryChatClientObservationConvention}.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryChatClientObservationConvention extends OpenTelemetryChatClientObservationConvention {

    private final JsonHelper jsonHelper = new JsonHelper();

    public OpenLLMetryChatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatClientObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(
                traceloopEntityName(context),
                traceloopSpanKind(context)
        );
    }

    @Override
    protected KeyValue stream(ChatClientObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_IS_STREAMING, String.valueOf(context.isStream()));
    }

    private KeyValue traceloopEntityName(ChatClientObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, "chat_client");
    }

    private KeyValue traceloopSpanKind(ChatClientObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND,
                OpenLLMetryAttributes.TraceloopSpanKind.WORKFLOW.getValue());
    }

    // HIGH CARDINALITY

    @Override
    protected KeyValues conversationId(KeyValues keyValues, ChatClientObservationContext context) {
        // Recorded both as gen_ai.conversation.id, as the Traceloop SDK does for conversations,
        // and as the session_id association property, which Traceloop uses to group sessions.
        String conversationId = GenAiConversations.conversationId(context);
        return conversationId != null
                ? keyValues.and(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey(), conversationId)
                        .and(OpenLLMetryAttributes.TRACELOOP_ASSOCIATION_PROPERTIES_SESSION_ID, conversationId)
                : keyValues;
    }

    @Override
    protected KeyValues inputMessages(KeyValues keyValues, ChatClientObservationContext context) {
        List<Message> messages = context.getRequest().prompt().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }
        var inputMessages = GenAiContent.fromMessages(messages);
        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, jsonHelper.toJson(inputMessages));
    }

    @Override
    protected KeyValues outputMessages(KeyValues keyValues, ChatClientObservationContext context) {
        if (context.getResponse() == null || context.getResponse().chatResponse() == null
                || CollectionUtils.isEmpty(context.getResponse().chatResponse().getResults())) {
            return keyValues;
        }
        var outputMessages = GenAiContent.fromGenerations(context.getResponse().chatResponse().getResults());
        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, jsonHelper.toJson(outputMessages));
    }

    @Override
    protected boolean recordsContentOnSpanAttributes() {
        // OpenLLMetry records content on span attributes only.
        return true;
    }

}
