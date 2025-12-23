package io.arconia.openinference.observation.instrumentation.ai;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.observation.conventions.SpringAiKind;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ChatClientObservationConvention} for OpenInference.
 */
public class OpenInferenceChatClientObservationConvention implements ChatClientObservationConvention {

    public static final String DEFAULT_NAME = "spring.ai.client";

    private final OpenInferenceTracingOptions tracingOptions;

    public OpenInferenceChatClientObservationConvention(OpenInferenceTracingOptions tracingOptions) {
        this.tracingOptions = tracingOptions;
    }

    @Override
    public String getName() {
        return DEFAULT_NAME;
    }

    @Override
    @Nullable
    public String getContextualName(ChatClientObservationContext context) {
        return "%s %s".formatted(context.getOperationMetadata().provider(), SpringAiKind.CHAT_CLIENT.value());
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatClientObservationContext context) {
        return KeyValues.of(aiOperationType());
    }

    private KeyValue aiOperationType() {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.CHAIN.getValue());
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = conversationId(keyValues, context);
        keyValues = input(keyValues, context);
        return keyValues;
    }

    private KeyValues conversationId(KeyValues keyValues, ChatClientObservationContext context) {
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

        if (tracingOptions.isHideInputs() || tracingOptions.isHideInputMessages() || tracingOptions.isHideInputText()) {
            return keyValues.and(SemanticConventions.INPUT_VALUE, OpenInferenceTracingOptions.REDACTED_PLACEHOLDER);
        } else {
            return keyValues.and(SemanticConventions.INPUT_VALUE, userInput);
        }
    }

}
