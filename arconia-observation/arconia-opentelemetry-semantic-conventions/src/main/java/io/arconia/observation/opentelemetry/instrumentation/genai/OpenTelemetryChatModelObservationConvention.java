package io.arconia.observation.opentelemetry.instrumentation.genai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import tools.jackson.core.type.TypeReference;

public final class OpenTelemetryChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private final OpenTelemetryGenAiOptions openTelemetryGenAiOptions;

    public OpenTelemetryChatModelObservationConvention(OpenTelemetryGenAiOptions openTelemetryGenAiOptions) {
        this.openTelemetryGenAiOptions = openTelemetryGenAiOptions;
    }

    @Override
    public String getContextualName(ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        String operationName = OpenTelemetryGenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType());
        if (options != null && StringUtils.hasText(options.getModel())) {
            return "%s %s".formatted(operationName, options.getModel());
        }
        return operationName;
    }

    // LOW CARDINALITY

    @Override
    protected KeyValue aiOperationType(ChatModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                OpenTelemetryGenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                OpenTelemetryGenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        // Content
        if (OpenTelemetryGenAiOptions.CaptureContentFormat.SPAN_ATTRIBUTES == openTelemetryGenAiOptions.getInference().getCaptureContent()) {
            keyValues = inputMessages(keyValues, context);
            keyValues = outputMessages(keyValues, context);
        }
        return keyValues;
    }

    // Request

    @Override
    protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (!openTelemetryGenAiOptions.getInference().isIncludeToolDefinitions()) {
            return keyValues;
        }

        if (!(context.getRequest().getOptions() instanceof ToolCallingChatOptions options)) {
            return keyValues;
        }

        List<Map<String, Object>> toolDefinitions = new ArrayList<>();

        List<ToolCallback> toolCallbacks = new ArrayList<>(options.getToolCallbacks());
        for (ToolCallback toolCallback : toolCallbacks) {
            Map<String, Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("name", toolCallback.getToolDefinition().name());
            toolDefinition.put("description", toolCallback.getToolDefinition().description());
            toolDefinition.put("parameters", JsonParser.fromJson(toolCallback.getToolDefinition().inputSchema(),
                    new TypeReference<Map<String, Object>>() {}));
            toolDefinitions.add(toolDefinition);
        }

        List<String> toolNames = new ArrayList<>(options.getToolNames());
        for (String toolName : toolNames) {
            Map<String, Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("name", toolName);
            toolDefinitions.add(toolDefinition);
        }

        return keyValues.and(GenAiMoreIncubatingAttributes.GEN_AI_TOOL_DEFINITIONS.getKey(), JsonParser.toJson(toolDefinitions));
    }

    // Content

    private KeyValues inputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        List<Message> messages = context.getRequest().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }

        var inputMessages = OpenTelemetryGenAiContent.fromMessages(messages);
        if (!inputMessages.isEmpty()) {
            return keyValues.and(GenAiMoreIncubatingAttributes.GEN_AI_INPUT_MESSAGES.getKey(), JsonParser.toJson(inputMessages));
        }
        return keyValues;
    }

    private KeyValues outputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }

        var outputMessages = OpenTelemetryGenAiContent.fromGenerations(context.getResponse().getResults());
        if (!outputMessages.isEmpty()) {
            return keyValues.and(GenAiMoreIncubatingAttributes.GEN_AI_OUTPUT_MESSAGES.getKey(), JsonParser.toJson(outputMessages));
        }
        return keyValues;
    }

}
