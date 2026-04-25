package io.arconia.observation.openinference.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

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
 * {@link ChatClientObservationConvention} for OpenInference.
 */
public class OpenInferenceChatClientObservationConvention extends DefaultChatClientObservationConvention {

    private final OpenInferenceOptions openInferenceOptions;

    public OpenInferenceChatClientObservationConvention(OpenInferenceOptions openInferenceOptions) {
        this.openInferenceOptions = openInferenceOptions;
    }

    protected KeyValue aiOperationType(ChatClientObservationContext context) {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.AGENT.getValue());
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = advisors(keyValues, context);
        keyValues = conversationId(keyValues, context);
        keyValues = input(keyValues, context);
        keyValues = output(keyValues, context);
        keyValues = tools(keyValues, context);
        return keyValues;
    }

    protected KeyValues conversationId(KeyValues keyValues, ChatClientObservationContext context) {
        if (CollectionUtils.isEmpty(context.getRequest().context())) {
            return keyValues;
        }

        var conversationIdValue = context.getRequest().context().get(ChatMemory.CONVERSATION_ID);

        if (!(conversationIdValue instanceof String conversationId) || !StringUtils.hasText(conversationId)) {
            return keyValues;
        }

        return keyValues.and(SemanticConventions.SESSION_ID, conversationId);
    }

    private KeyValues input(KeyValues keyValues, ChatClientObservationContext context) {
        String userInput = context.getRequest().prompt().getUserMessage().getText();

        if (!StringUtils.hasText(userInput)) {
            return keyValues;
        }

        if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideInputMessages() || openInferenceOptions.isHideInputText()) {
            return keyValues.and(SemanticConventions.INPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        } else {
            return keyValues.and(SemanticConventions.INPUT_VALUE, userInput);
        }
    }

    private KeyValues output(KeyValues keyValues, ChatClientObservationContext context) {
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

        if (openInferenceOptions.isHideOutputs() || openInferenceOptions.isHideOutputMessages() || openInferenceOptions.isHideOutputText()) {
            return keyValues.and(SemanticConventions.OUTPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        } else {
            return keyValues.and(SemanticConventions.OUTPUT_VALUE, outputText);
        }
    }

}
