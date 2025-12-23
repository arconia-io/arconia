package io.arconia.openinference.observation.instrumentation.ai;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.observation.conventions.SpringAiKind;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.util.Assert;

/**
 * {@link ToolCallingObservationConvention} for OpenInference.
 */
public class OpenInferenceToolCallingObservationConvention implements ToolCallingObservationConvention {

    public static final String DEFAULT_NAME = "spring.ai.tool";

    private final OpenInferenceTracingOptions tracingOptions;

    public OpenInferenceToolCallingObservationConvention(OpenInferenceTracingOptions tracingOptions) {
        this.tracingOptions = tracingOptions;
    }

    @Override
    public String getName() {
        return DEFAULT_NAME;
    }

    @Override
    @Nullable
    public String getContextualName(ToolCallingObservationContext context) {
        Assert.notNull(context, "context cannot be null");
        String toolName = context.getToolDefinition().name();
        return "%s %s".formatted(SpringAiKind.TOOL_CALL.value(), toolName);
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return KeyValues.of(aiOperationType(), toolDefinitionName(context));
    }

    private KeyValue aiOperationType() {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.TOOL.getValue());
    }

    private KeyValue toolDefinitionName(ToolCallingObservationContext context) {
        String toolName = context.getToolDefinition().name();
        return KeyValue.of(SemanticConventions.TOOL_NAME, toolName);
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ToolCallingObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = toolDefinitionDescription(keyValues, context);
        keyValues = toolCallArguments(keyValues, context);
        keyValues = toolCallResult(keyValues, context);
        return keyValues;
    }

    private KeyValues toolDefinitionDescription(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolDescription = context.getToolDefinition().description();
        return keyValues.and(
                SemanticConventions.TOOL_DESCRIPTION,
                toolDescription);
    }

    private KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallArguments = context.getToolCallArguments();

        if (tracingOptions.isHideInputs()) {
            return keyValues.and(SemanticConventions.INPUT_VALUE, OpenInferenceTracingOptions.REDACTED_PLACEHOLDER);
        } else {
            return keyValues.and(SemanticConventions.INPUT_VALUE, toolCallArguments)
                    .and(SemanticConventions.INPUT_MIME_TYPE, "application/json");
        }
    }

    private KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();

        if (tracingOptions.isHideOutputs()) {
            return keyValues.and(SemanticConventions.OUTPUT_VALUE, OpenInferenceTracingOptions.REDACTED_PLACEHOLDER);
        } else {
            return keyValues.and(SemanticConventions.OUTPUT_VALUE, toolCallResult != null ? toolCallResult : "{}")
                    .and(SemanticConventions.OUTPUT_MIME_TYPE, "application/json");
        }
    }

}
