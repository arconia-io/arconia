package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * Base {@link ChatClientObservationConvention} implementing OpenTelemetry Semantic Conventions for Generative AI,
 * reporting a ChatClient call as an agent invocation. Subclasses for other flavors override only what differs.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-agent-spans/">OpenTelemetry Semantic Conventions for GenAI Agent Spans</a>
 */
public class OpenTelemetryChatClientObservationConvention extends DefaultChatClientObservationConvention {

    private final JsonHelper jsonHelper = new JsonHelper();

    private final OpenTelemetryAiConventionsProperties properties;

    public OpenTelemetryChatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getContextualName(ChatClientObservationContext context) {
        return GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.INVOKE_AGENT;
    }

    @Override
    protected KeyValue aiOperationType(ChatClientObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.INVOKE_AGENT);
    }

    @Override
    protected KeyValue aiProvider(ChatClientObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        keyValues = requestModel(keyValues, context);
        if (properties.isCaptureMessageContent() && recordsContentOnSpanAttributes()) {
            keyValues = inputMessages(keyValues, context);
            keyValues = outputMessages(keyValues, context);
        }
        return keyValues;
    }

    /**
     * Whether the captured content is recorded on the span attributes. The conventions define no event
     * for agent invocations, so the content is recorded only when the inference content goes to span
     * attributes too. When it goes to events, the model calls already carry it.
     * Flavors recording content on span attributes only override it.
     */
    protected boolean recordsContentOnSpanAttributes() {
        return properties.getInferenceContentDestination()
                == OpenTelemetryAiConventionsProperties.InferenceContentDestination.SPAN_ATTRIBUTES;
    }

    protected KeyValues requestModel(KeyValues keyValues, ChatClientObservationContext context) {
        ChatOptions options = context.getRequest().prompt().getOptions();
        if (options == null || !StringUtils.hasText(options.getModel())) {
            return keyValues;
        }
        return keyValues.and(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), options.getModel());
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
            return keyValues.and(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey(), jsonHelper.toJson(inputMessages));
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
            return keyValues.and(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey(), jsonHelper.toJson(outputMessages));
        }
        return keyValues;
    }

}
