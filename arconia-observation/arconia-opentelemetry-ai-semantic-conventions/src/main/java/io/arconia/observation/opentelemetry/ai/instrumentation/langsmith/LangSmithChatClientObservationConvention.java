package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.CollectionUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConversations;

/**
 * LangSmith flavor of {@link OpenTelemetryChatClientObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
public final class LangSmithChatClientObservationConvention extends OpenTelemetryChatClientObservationConvention {

    public LangSmithChatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatClientObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(langSmithSpanKind(context));
    }

    @Override
    protected KeyValue aiProvider(ChatClientObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(),
                GenAiConventionsConverter.toLangSmithProviderName(context.getOperationMetadata().provider()));
    }

    private KeyValue langSmithSpanKind(ChatClientObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                GenAiConventionsConverter.toLangSmithSpanKind(context.getOperationMetadata().operationType()));
    }

    // HIGH CARDINALITY

    @Override
    protected KeyValues conversationId(KeyValues keyValues, ChatClientObservationContext context) {
        String conversationId = GenAiConversations.conversationId(context);
        return conversationId != null
                ? keyValues.and(LangSmithAttributes.LANGSMITH_METADATA_THREAD_ID.getKey(), conversationId)
                : keyValues;
    }

    @Override
    protected boolean recordsContentOnSpanAttributes() {
        // LangSmith records content on span attributes only.
        return true;
    }

    @Override
    protected KeyValues inputMessages(KeyValues keyValues, ChatClientObservationContext context) {
        List<Message> messages = context.getRequest().prompt().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }
        return keyValues.and(LangSmithAttributes.GEN_AI_PROMPT.getKey(),
                jsonHelper.toJson(LangSmithMessages.fromMessages(messages)));
    }

    @Override
    protected KeyValues outputMessages(KeyValues keyValues, ChatClientObservationContext context) {
        ChatResponse chatResponse = context.getResponse() != null ? context.getResponse().chatResponse() : null;
        if (chatResponse == null || CollectionUtils.isEmpty(chatResponse.getResults())) {
            return keyValues;
        }
        return keyValues.and(LangSmithAttributes.GEN_AI_COMPLETION.getKey(),
                jsonHelper.toJson(LangSmithMessages.fromGenerations(chatResponse.getResults())));
    }

}
