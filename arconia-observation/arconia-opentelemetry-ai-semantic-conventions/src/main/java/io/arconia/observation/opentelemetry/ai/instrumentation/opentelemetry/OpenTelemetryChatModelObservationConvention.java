package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiContent;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * Base {@link ChatModelObservationConvention} implementing OpenTelemetry Semantic Conventions for Generative AI.
 * Subclasses for other flavors override only what differs.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/">OpenTelemetry Semantic Conventions for Generative AI</a>
 */
public class OpenTelemetryChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private final JsonHelper jsonHelper = new JsonHelper();

    private final OpenTelemetryAiConventionsProperties properties;

    public OpenTelemetryChatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        this.properties = properties;
    }

    protected OpenTelemetryAiConventionsProperties getProperties() {
        return properties;
    }

    @Override
    public String getContextualName(ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        String operationName = GenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType());
        if (options != null && StringUtils.hasText(options.getModel())) {
            return "%s %s".formatted(operationName, options.getModel());
        }
        return operationName;
    }

    // LOW CARDINALITY

    @Override
    protected KeyValue aiOperationType(ChatModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                GenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        keyValues = outputType(keyValues, context);
        if (OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_ATTRIBUTES == properties.getCaptureContent()) {
            keyValues = inputMessages(keyValues, context);
            keyValues = outputMessages(keyValues, context);
        }
        return keyValues;
    }

    private KeyValues outputType(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        String outputType = GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.TEXT;
        if (options instanceof StructuredOutputChatOptions structuredOutputOptions) {
            outputType = structuredOutputOptions.getOutputSchema() != null
                    ? GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.JSON
                    : GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.TEXT;
        }
        return keyValues.and(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey(), outputType);
    }

    // Request

    @Override
    protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (!properties.isIncludeToolDefinitions()) {
            return keyValues;
        }
        if (!(context.getRequest().getOptions() instanceof ToolCallingChatOptions options)) {
            return keyValues;
        }
        if (CollectionUtils.isEmpty(options.getToolCallbacks())) {
            return keyValues;
        }
        return keyValues.and(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS.getKey(),
                jsonHelper.toJson(buildToolDefinitions(options.getToolCallbacks())));
    }

    protected List<Map<String, Object>> buildToolDefinitions(List<ToolCallback> toolCallbacks) {
        List<Map<String, Object>> toolDefinitions = new ArrayList<>();
        for (ToolCallback toolCallback : toolCallbacks) {
            Map<String, Object> toolDef = new HashMap<>();
            toolDef.put("type", "function");
            toolDef.put("name", toolCallback.getToolDefinition().name());
            toolDef.put("description", toolCallback.getToolDefinition().description());
            toolDef.put("parameters", Objects.requireNonNullElse(jsonHelper.fromJson(toolCallback.getToolDefinition().inputSchema(),
                    new ParameterizedTypeReference<Map<String, Object>>() {}), Map.of()));
            toolDefinitions.add(toolDef);
        }
        return toolDefinitions;
    }

    // Content

    protected KeyValues inputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        List<Message> messages = context.getRequest().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }
        var inputMessages = GenAiContent.fromMessages(messages);
        if (!inputMessages.isEmpty()) {
            return keyValues.and(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey(), jsonHelper.toJson(inputMessages));
        }
        return keyValues;
    }

    protected KeyValues outputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }
        var outputMessages = GenAiContent.fromGenerations(context.getResponse().getResults());
        if (!outputMessages.isEmpty()) {
            return keyValues.and(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey(), jsonHelper.toJson(outputMessages));
        }
        return keyValues;
    }

}
