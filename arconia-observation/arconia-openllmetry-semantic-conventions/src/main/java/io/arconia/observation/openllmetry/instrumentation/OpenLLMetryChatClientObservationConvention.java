package io.arconia.observation.openllmetry.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ChatClientObservationConvention} for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public class OpenLLMetryChatClientObservationConvention extends DefaultChatClientObservationConvention {

    private final OpenLLMetryOptions openLLMetryOptions;

    public OpenLLMetryChatClientObservationConvention(OpenLLMetryOptions openLLMetryOptions) {
        this.openLLMetryOptions = openLLMetryOptions;
    }

    protected KeyValue aiOperationType(ChatClientObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.SPAN_KIND_WORKFLOW);
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = advisors(keyValues, context);
        keyValues = associationProperties(keyValues, context);
        keyValues = entityInput(keyValues, context);
        keyValues = entityOutput(keyValues, context);
        keyValues = tools(keyValues, context);
        return keyValues;
    }

    protected KeyValues associationProperties(KeyValues keyValues, ChatClientObservationContext context) {
        if (CollectionUtils.isEmpty(context.getRequest().context())) {
            return keyValues;
        }

        var conversationIdValue = context.getRequest().context().get(ChatMemory.CONVERSATION_ID);

        if (!(conversationIdValue instanceof String conversationId) || !StringUtils.hasText(conversationId)) {
            return keyValues;
        }

        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ASSOCIATION_PROPERTIES + ".conversation_id",
                conversationId);
    }

    private KeyValues entityInput(KeyValues keyValues, ChatClientObservationContext context) {
        String userInput = context.getRequest().prompt().getUserMessage().getText();

        if (!StringUtils.hasText(userInput)) {
            return keyValues;
        }

        if (!openLLMetryOptions.isTraceContent()) {
            return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER);
        }

        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, userInput);
    }

    private KeyValues entityOutput(KeyValues keyValues, ChatClientObservationContext context) {
        if (context.getResponse() == null || context.getResponse().chatResponse() == null) {
            return keyValues;
        }

        ChatResponse chatResponse = context.getResponse().chatResponse();
        Generation result = chatResponse.getResult();
        if (result == null) {
            return keyValues;
        }

        String outputText = result.getOutput().getText();
        if (!StringUtils.hasText(outputText)) {
            return keyValues;
        }

        if (!openLLMetryOptions.isTraceContent()) {
            return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER);
        }

        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, outputText);
    }

}
