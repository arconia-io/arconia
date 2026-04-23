package io.arconia.observation.openinference.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;

/**
 * {@link ToolCallingObservationConvention} for OpenInference.
 */
public class OpenInferenceToolCallingObservationConvention extends DefaultToolCallingObservationConvention {

    private final OpenInferenceOptions tracingOptions;

    public OpenInferenceToolCallingObservationConvention(OpenInferenceOptions tracingOptions) {
        this.tracingOptions = tracingOptions;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return KeyValues.of(aiOperationType(), aiProvider(context), toolDefinitionName(context));
    }

    protected KeyValue aiOperationType() {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.TOOL.getValue());
    }

    protected KeyValue toolDefinitionName(ToolCallingObservationContext context) {
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

    protected KeyValues toolDefinitionDescription(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolDescription = context.getToolDefinition().description();
        return keyValues.and(
                SemanticConventions.TOOL_DESCRIPTION,
                toolDescription);
    }

    private KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallArguments = context.getToolCallArguments();

        if (tracingOptions.isHideInputs()) {
            return keyValues.and(SemanticConventions.INPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        } else {
            return keyValues.and(SemanticConventions.INPUT_VALUE, toolCallArguments)
                    .and(SemanticConventions.INPUT_MIME_TYPE, "application/json");
        }
    }

    private KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();

        if (tracingOptions.isHideOutputs()) {
            return keyValues.and(SemanticConventions.OUTPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        } else {
            return keyValues.and(SemanticConventions.OUTPUT_VALUE, toolCallResult != null ? toolCallResult : "{}")
                    .and(SemanticConventions.OUTPUT_MIME_TYPE, "application/json");
        }
    }

}
