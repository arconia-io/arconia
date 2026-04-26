package io.arconia.observation.langsmith.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.JsonParser;

import tools.jackson.core.type.TypeReference;

public final class LangSmithChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private final LangSmithOptions langSmithOptions;

    public LangSmithChatModelObservationConvention(LangSmithOptions langSmithOptions) {
        this.langSmithOptions = langSmithOptions;
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ChatModelObservationContext context) {
        return KeyValues.of(aiOperationType(context), aiProvider(context), langSmithSpanKind(context),
                requestModel(context), responseModel(context));
    }

    @Override
    protected KeyValue aiProvider(ChatModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(),
                LangSmithConventionsConverter.toSystemName(context.getOperationMetadata().provider()));
    }

    private KeyValue langSmithSpanKind(ChatModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                LangSmithConventionsConverter.toSpanKind(context.getOperationMetadata().operationType()));
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        var keyValues = KeyValues.empty();
        // Request
        keyValues = requestFrequencyPenalty(keyValues, context);
        keyValues = requestMaxTokens(keyValues, context);
        keyValues = requestPresencePenalty(keyValues, context);
        keyValues = requestStopSequences(keyValues, context);
        keyValues = requestTemperature(keyValues, context);
        keyValues = requestTools(keyValues, context);
        keyValues = requestTopK(keyValues, context);
        keyValues = requestTopP(keyValues, context);
        return keyValues;
    }

    // Request

    @Override
    protected KeyValues requestTools(KeyValues keyValues, ChatModelObservationContext context) {
        if (!langSmithOptions.getInference().isIncludeToolDefinitions()) {
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

        return keyValues.and(LangSmithAttributes.TOOLS.getKey(), JsonParser.toJson(toolDefinitions));
    }

}
