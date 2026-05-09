package io.arconia.observation.openllmetry.instrumentation;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.util.json.JsonParser;
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

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatClientObservationContext context) {
        return super.getLowCardinalityKeyValues(context)
                .and(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, "chat_client");
    }

    @Override
    protected KeyValue aiOperationType(ChatClientObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.TraceloopSpanKind.WORKFLOW.getValue());
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        // Content
        if (openLLMetryOptions.getInference().isIncludeContent()) {
            keyValues = entityInput(keyValues, context);
            keyValues = entityOutput(keyValues, context);
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

    // Content

    private KeyValues entityInput(KeyValues keyValues, ChatClientObservationContext context) {
        List<Message> messages = context.getRequest().prompt().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }

        var inputMessages = OpenTelemetryGenAiContent.fromMessages(messages);
        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, JsonParser.toJson(inputMessages));
    }

    private KeyValues entityOutput(KeyValues keyValues, ChatClientObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().chatResponse().getResults())) {
            return keyValues;
        }

        var outputMessages = OpenTelemetryGenAiContent.fromGenerations(context.getResponse().chatResponse().getResults());
        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, JsonParser.toJson(outputMessages));
    }

}
