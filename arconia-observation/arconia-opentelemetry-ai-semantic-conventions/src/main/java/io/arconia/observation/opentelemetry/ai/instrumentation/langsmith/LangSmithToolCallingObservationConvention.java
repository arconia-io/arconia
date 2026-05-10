package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import java.util.LinkedHashMap;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.util.json.JsonParser;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * LangSmith flavor of {@link OpenTelemetryToolCallingObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
public final class LangSmithToolCallingObservationConvention extends OpenTelemetryToolCallingObservationConvention {

    public LangSmithToolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(langSmithSpanKind(context));
    }

    @Override
    protected KeyValue aiProvider(ToolCallingObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(),
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    private KeyValue langSmithSpanKind(ToolCallingObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                GenAiConventionsConverter.toLangSmithSpanKind(context.getOperationMetadata().operationType()));
    }

    // HIGH CARDINALITY

    @Override
    protected KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallArguments = context.getToolCallArguments();
        return keyValues.and(
                LangSmithAttributes.GEN_AI_PROMPT.getKey(),
                toolCallArguments);
    }

    @Override
    protected KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
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
     * to be a JSON object — non-object values are wrapped as {@code {"output": <value>}}.
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
