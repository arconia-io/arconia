package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

public class OpenTelemetryChatClientObservationConvention extends DefaultChatClientObservationConvention {

    private final OpenTelemetryAiConventionsProperties properties;

    public OpenTelemetryChatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        this.properties = properties;
    }

    protected OpenTelemetryAiConventionsProperties getProperties() {
        return properties;
    }

    @Override
    protected KeyValue aiOperationType(ChatClientObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                GenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(ChatClientObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    protected KeyValue stream(ChatClientObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_STREAM.getKey(), String.valueOf(context.isStream()));
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        if (OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_ATTRIBUTES == properties.getCaptureContent()) {
            keyValues = inputMessages(keyValues, context);
            keyValues = outputMessages(keyValues, context);
        }
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
        return keyValues.and(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey(), conversationId);
    }

    protected KeyValues inputMessages(KeyValues keyValues, ChatClientObservationContext context) {
        List<Message> messages = context.getRequest().prompt().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }
        var inputMessages = GenAiContent.fromMessages(messages);
        if (!inputMessages.isEmpty()) {
            return keyValues.and(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey(), JsonParser.toJson(inputMessages));
        }
        return keyValues;
    }

    protected KeyValues outputMessages(KeyValues keyValues, ChatClientObservationContext context) {
        if (context.getResponse() == null || context.getResponse().chatResponse() == null
                || CollectionUtils.isEmpty(context.getResponse().chatResponse().getResults())) {
            return keyValues;
        }
        var outputMessages = GenAiContent.fromGenerations(context.getResponse().chatResponse().getResults());
        if (!outputMessages.isEmpty()) {
            return keyValues.and(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey(), JsonParser.toJson(outputMessages));
        }
        return keyValues;
    }

}
