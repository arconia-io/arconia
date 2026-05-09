package io.arconia.observation.openllmetry.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;

import tools.jackson.core.type.TypeReference;

/**
 * {@link ChatModelObservationConvention} for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private final OpenLLMetryOptions openLLMetryOptions;

    public OpenLLMetryChatModelObservationConvention(OpenLLMetryOptions openLLMetryOptions) {
        this.openLLMetryOptions = openLLMetryOptions;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatModelObservationContext context) {
        return KeyValues.of(aiOperationType(context), aiProvider(context), requestModel(context),
                responseModel(context));
    }

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM,
                OpenLLMetryConventionsConverters.toSystemName(context.getOperationMetadata().provider()));
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        keyValues = outputType(keyValues, context);
        // Content
        if (openLLMetryOptions.getInference().isIncludeContent()) {
            keyValues = inputMessages(keyValues, context);
            keyValues = outputMessages(keyValues, context);
        }
        return keyValues;
    }

    private KeyValues outputType(KeyValues keyValues, ChatModelObservationContext context) {
        ChatOptions options = context.getRequest().getOptions();
        if (options instanceof StructuredOutputChatOptions structuredOutputOptions) {
            var outputType = structuredOutputOptions.getOutputSchema() != null ? GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.JSON
                    : GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.TEXT;
            return keyValues.and(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey(), outputType);
        }
        return keyValues;
    }

    // Request

    @Override
    protected KeyValues requestStream(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.isStreaming()) {
            return keyValues.and(OpenLLMetryAttributes.GEN_AI_IS_STREAMING,
                    String.valueOf(true));
        }
        return keyValues;
    }

    @Override
    protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (!openLLMetryOptions.getInference().isIncludeToolDefinitions()) {
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

        return keyValues.and(OpenLLMetryAttributes.GEN_AI_TOOL_DEFINITIONS, JsonParser.toJson(toolDefinitions));
    }

    // Content

    private KeyValues inputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        List<Message> messages = context.getRequest().getInstructions();
        if (CollectionUtils.isEmpty(messages)) {
            return keyValues;
        }

        var inputMessages = OpenTelemetryGenAiContent.fromMessages(messages);
        return keyValues.and(OpenLLMetryAttributes.GEN_AI_INPUT_MESSAGES, JsonParser.toJson(inputMessages));
    }

    private KeyValues outputMessages(KeyValues keyValues, ChatModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }

        var outputMessages = OpenTelemetryGenAiContent.fromGenerations(context.getResponse().getResults());
        return keyValues.and(OpenLLMetryAttributes.GEN_AI_OUTPUT_MESSAGES, JsonParser.toJson(outputMessages));
    }

}
