package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import java.util.LinkedHashMap;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import tools.jackson.databind.util.RawValue;

import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.util.JsonHelper;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiJson;

/**
 * LangSmith flavor of {@link OpenTelemetryToolCallingObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
public final class LangSmithToolCallingObservationConvention extends OpenTelemetryToolCallingObservationConvention {

    private final JsonHelper jsonHelper = new JsonHelper();

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
                GenAiConventionsConverter.toLangSmithProviderName(context.getOperationMetadata().provider()));
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
     * Ensures the value is a JSON object, which LangSmith expects {@code gen_ai.completion} to be. A JSON object
     * is recorded as it is. Any other JSON value, such as an array or {@code null}, is embedded as it is in
     * {@code {"output": <value>}}, and plain text is wrapped the same way as a string. The value is validated
     * without building a tree, since it's never parsed, only embedded.
     */
    private String toJsonObject(String value) {
        boolean json = GenAiJson.isValid(value);
        if (json && GenAiJson.isObject(value)) {
            return value;
        }
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("output", json ? new RawValue(value) : value);
        return jsonHelper.toJson(wrapper);
    }

}
