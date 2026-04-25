package io.arconia.observation.opentelemetry.instrumentation.genai;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.util.Assert;

public final class OpenTelemetryToolCallingModelObservationConvention extends DefaultToolCallingObservationConvention {

    private static final KeyValue OPERATION_NAME = KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
            GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.EXECUTE_TOOL);

    private static final KeyValue TOOL_TYPE = KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_TYPE.getKey(),
            "function");

    private static final KeyValue TOOL_CALL_RESULT_NONE = KeyValue
            .of(GenAiMoreIncubatingAttributes.GEN_AI_TOOL_CALL_RESULT.getKey(), KeyValue.NONE_VALUE);

    private final OpenTelemetryGenAiOptions openTelemetryGenAiOptions;

    public OpenTelemetryToolCallingModelObservationConvention(OpenTelemetryGenAiOptions openTelemetryGenAiOptions) {
        this.openTelemetryGenAiOptions = openTelemetryGenAiOptions;
    }

    @Override
    public String getContextualName(ToolCallingObservationContext context) {
        Assert.notNull(context, "context cannot be null");
        String operationName = GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.EXECUTE_TOOL;
        String toolName = context.getToolDefinition().name();
        return "%s %s".formatted(operationName, toolName);
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return KeyValues.of(aiOperationType(context), toolType(context), toolDefinitionName(context));
    }

    @Override
    protected KeyValue aiOperationType(ToolCallingObservationContext context) {
        return OPERATION_NAME;
    }

    private KeyValue toolType(ToolCallingObservationContext context) {
        return TOOL_TYPE;
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
        if (openTelemetryGenAiOptions.getToolExecution().isIncludeContent()) {
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
                GenAiMoreIncubatingAttributes.GEN_AI_TOOL_CALL_ARGUMENTS.getKey(),
                toolCallArguments);
    }

    private KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();
        if (toolCallResult != null) {
            return keyValues.and(
                    GenAiMoreIncubatingAttributes.GEN_AI_TOOL_CALL_RESULT.getKey(),
                    toolCallResult);
        }
        return keyValues.and(TOOL_CALL_RESULT_NONE);
    }

}
