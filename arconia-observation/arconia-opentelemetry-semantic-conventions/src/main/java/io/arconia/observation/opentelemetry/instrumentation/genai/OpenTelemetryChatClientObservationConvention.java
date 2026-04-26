package io.arconia.observation.opentelemetry.instrumentation.genai;

import java.util.List;

import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class OpenTelemetryChatClientObservationConvention extends DefaultChatClientObservationConvention {

    private final OpenTelemetryGenAiOptions openTelemetryGenAiOptions;

    public OpenTelemetryChatClientObservationConvention(OpenTelemetryGenAiOptions openTelemetryGenAiOptions) {
        this.openTelemetryGenAiOptions = openTelemetryGenAiOptions;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        // Content
        if (OpenTelemetryGenAiOptions.CaptureContentFormat.SPAN_ATTRIBUTES == openTelemetryGenAiOptions.getInference().getCaptureContent()) {
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

        return keyValues.and(
                GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey(),
                conversationId);
    }

    private KeyValues inputMessages(KeyValues keyValues, ChatClientObservationContext context) {
        List<Message> messages = context.getRequest().prompt().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }

        var inputMessages = OpenTelemetryGenAiContent.fromMessages(messages);
        if (!inputMessages.isEmpty()) {
            return keyValues.and(GenAiMoreIncubatingAttributes.GEN_AI_INPUT_MESSAGES.getKey(), JsonParser.toJson(inputMessages));
        }
        return keyValues;
    }

    private KeyValues outputMessages(KeyValues keyValues, ChatClientObservationContext context) {
        if (context.getResponse() == null || context.getResponse().chatResponse() == null
                || CollectionUtils.isEmpty(context.getResponse().chatResponse().getResults())) {
            return keyValues;
        }

        var outputMessages = OpenTelemetryGenAiContent.fromGenerations(context.getResponse().chatResponse().getResults());
        if (!outputMessages.isEmpty()) {
            return keyValues.and(GenAiMoreIncubatingAttributes.GEN_AI_OUTPUT_MESSAGES.getKey(), JsonParser.toJson(outputMessages));
        }
        return keyValues;
    }

}
