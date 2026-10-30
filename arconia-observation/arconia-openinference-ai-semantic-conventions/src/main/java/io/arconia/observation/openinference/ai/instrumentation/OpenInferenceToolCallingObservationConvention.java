package io.arconia.observation.openinference.ai.instrumentation;


import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ToolCallingObservationConvention} for OpenInference.
 */
public final class OpenInferenceToolCallingObservationConvention extends DefaultToolCallingObservationConvention {


    private final OpenInferenceOptions openInferenceOptions;

    public OpenInferenceToolCallingObservationConvention(OpenInferenceOptions openInferenceOptions) {
        this.openInferenceOptions = openInferenceOptions;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return KeyValues.of(aiOperationType(context), aiProvider(context), toolDefinitionName(context));
    }

    @Override
    protected KeyValue aiOperationType(ToolCallingObservationContext context) {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.TOOL.getValue());
    }

    @Override
    protected KeyValue toolDefinitionName(ToolCallingObservationContext context) {
        String toolName = context.getToolDefinition().name();
        return KeyValue.of(SemanticConventions.TOOL_NAME, toolName);
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ToolCallingObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = toolDefinitionDescription(keyValues, context);
        keyValues = toolParameters(keyValues, context);
        keyValues = toolId(keyValues, context);
        keyValues = toolCallArguments(keyValues, context);
        keyValues = toolCallResult(keyValues, context);
        return keyValues;
    }

    @Override
    protected KeyValues toolDefinitionDescription(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolDescription = context.getToolDefinition().description();
        return keyValues.and(
                SemanticConventions.TOOL_DESCRIPTION,
                toolDescription);
    }

    private KeyValues toolParameters(KeyValues keyValues, ToolCallingObservationContext context) {
        String inputSchema = context.getToolDefinition().inputSchema();
        if (!StringUtils.hasText(inputSchema)) {
            return keyValues;
        }
        return keyValues.and(SemanticConventions.TOOL_PARAMETERS, inputSchema);
    }

    private KeyValues toolId(KeyValues keyValues, ToolCallingObservationContext context) {
        if (!StringUtils.hasText(context.getToolCallId())) {
            return keyValues;
        }
        return keyValues.and(SemanticConventions.TOOL_ID, context.getToolCallId());
    }

    private KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallArguments = context.getToolCallArguments();

        if (openInferenceOptions.isHideInputs()) {
            return keyValues.and(SemanticConventions.INPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER)
                    .and(SemanticConventions.INPUT_MIME_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);
        } else {
            return keyValues.and(SemanticConventions.INPUT_VALUE, toolCallArguments)
                    .and(SemanticConventions.INPUT_MIME_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
        }
    }

    private KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();

        if (openInferenceOptions.isHideOutputs()) {
            return keyValues.and(SemanticConventions.OUTPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER)
                    .and(SemanticConventions.OUTPUT_MIME_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);
        }
        if (toolCallResult == null) {
            return keyValues;
        }
        return keyValues.and(SemanticConventions.OUTPUT_VALUE, toolCallResult)
                .and(SemanticConventions.OUTPUT_MIME_TYPE, isJson(toolCallResult)
                        ? MimeTypeUtils.APPLICATION_JSON_VALUE : MimeTypeUtils.TEXT_PLAIN_VALUE);
    }

    /**
     * Whether the value looks like a JSON object or array, as opposed to plain text such as a bare string.
     * It only picks the MIME type of the output, so the first and last characters are enough to tell:
     * parsing a tool result, which can be large, would be wasted work.
     */
    private static boolean isJson(String value) {
        return OpenInferenceJson.isObjectOrArray(value);
    }

}
