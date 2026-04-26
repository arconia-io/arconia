package io.arconia.observation.langsmith.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class LangSmithChatClientObservationConvention extends DefaultChatClientObservationConvention {

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatClientObservationContext context) {
        return KeyValues.of(aiOperationType(context), aiProvider(context), langSmithSpanKind(context));
    }

    @Override
    protected KeyValue aiOperationType(ChatClientObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                LangSmithConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    private KeyValue langSmithSpanKind(ChatClientObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                LangSmithConventionsConverter.toSpanKind(context.getOperationMetadata().operationType()));
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = conversationId(keyValues, context);
        return keyValues;
    }

    @Override
    protected KeyValues conversationId(KeyValues keyValues, ChatClientObservationContext context) {
        if (CollectionUtils.isEmpty(context.getRequest().context())) {
            return keyValues;
        }

        var conversationIdValue = context.getRequest().context().get(ChatMemory.CONVERSATION_ID);

        if (!(conversationIdValue instanceof String conversationId) || !StringUtils.hasText(conversationId)) {
            return keyValues;
        }

        return keyValues.and(
                LangSmithAttributes.LANGSMITH_TRACE_SESSION_ID.getKey(),
                conversationId);
    }

}
