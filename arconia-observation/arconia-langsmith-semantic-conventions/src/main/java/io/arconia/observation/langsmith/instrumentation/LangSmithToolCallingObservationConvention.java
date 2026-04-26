package io.arconia.observation.langsmith.instrumentation;

import java.util.LinkedHashMap;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.Assert;

public final class LangSmithToolCallingObservationConvention extends DefaultToolCallingObservationConvention {

    private static final KeyValue SPAN_KIND_TOOL = KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "tool");

    private final LangSmithOptions langSmithOptions;

    public LangSmithToolCallingObservationConvention(LangSmithOptions langSmithOptions) {
        this.langSmithOptions = langSmithOptions;
    }

    @Override
    public String getContextualName(ToolCallingObservationContext context) {
        Assert.notNull(context, "context cannot be null");
        String toolName = context.getToolDefinition().name();
        return "execute_tool %s".formatted(toolName);
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return KeyValues.of(aiOperationType(context), langSmithSpanKind(), toolDefinitionName(context));
    }

    @Override
    protected KeyValue aiOperationType(ToolCallingObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.EXECUTE_TOOL);
    }

    private KeyValue langSmithSpanKind() {
        return SPAN_KIND_TOOL;
    }

    @Override
    protected KeyValue toolDefinitionName(ToolCallingObservationContext context) {
        String toolName = context.getToolDefinition().name();
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_NAME.getKey(), toolName);
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ToolCallingObservationContext context) {
        var keyValues = KeyValues.empty();
        // Metadata
        keyValues = toolDefinitionDescription(keyValues, context);
        // Content
        if (langSmithOptions.getToolExecution().isIncludeContent()) {
            keyValues = toolCallArguments(keyValues, context);
            keyValues = toolCallResult(keyValues, context);
        }
        return keyValues;
    }

    // Metadata

    @Override
    protected KeyValues toolDefinitionDescription(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolDescription = context.getToolDefinition().description();
        return keyValues.and(
                GenAiIncubatingAttributes.GEN_AI_TOOL_DESCRIPTION.getKey(),
                toolDescription);
    }

    // Content

    private KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallArguments = context.getToolCallArguments();
        return keyValues.and(
                LangSmithAttributes.GEN_AI_PROMPT.getKey(),
                toolCallArguments);
    }

    private KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();
        if (toolCallResult != null) {
            return keyValues.and(
                    LangSmithAttributes.GEN_AI_COMPLETION.getKey(),
                    toJsonObject(toolCallResult));
        }
        return keyValues;
    }

    /**
     * Ensures the value is a JSON object string. LangSmith expects {@code gen_ai.completion}
     * to be a JSON object. Therefore, non-object values (arrays, plain strings) get wrapped
     * as {@code {"text": "..."}} which doesn't render well.
     * <p>
     * If the value is already a JSON object, it is returned as-is. Otherwise, it is
     * wrapped as {@code {"output": <value>}}, parsing the value first to avoid
     * double-escaping.
     */
    private static String toJsonObject(String value) {
        try {
            Object parsed = JsonParser.fromJson(value, Object.class);
            if (parsed instanceof Map) {
                return value;
            }
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("output", parsed);
            return JsonParser.toJson(wrapper);
        } catch (Exception e) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("output", value);
            return JsonParser.toJson(wrapper);
        }
    }

}
