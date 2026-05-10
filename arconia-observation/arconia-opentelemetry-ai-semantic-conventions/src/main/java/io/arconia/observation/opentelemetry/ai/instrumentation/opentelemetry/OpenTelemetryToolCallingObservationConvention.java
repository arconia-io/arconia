package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.util.Assert;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

public class OpenTelemetryToolCallingObservationConvention extends DefaultToolCallingObservationConvention {

    private final OpenTelemetryAiConventionsProperties properties;

    public OpenTelemetryToolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        this.properties = properties;
    }

    protected OpenTelemetryAiConventionsProperties getProperties() {
        return properties;
    }

    @Override
    public String getContextualName(ToolCallingObservationContext context) {
        Assert.notNull(context, "context cannot be null");
        String operationName = GenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType());
        String toolName = context.getToolDefinition().name();
        return "%s %s".formatted(operationName, toolName);
    }

    // LOW CARDINALITY

    @Override
    protected KeyValue aiOperationType(ToolCallingObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                GenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(ToolCallingObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    protected KeyValue toolDefinitionName(ToolCallingObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_NAME.getKey(), context.getToolDefinition().name());
    }

    @Override
    protected KeyValue toolType(ToolCallingObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_TYPE.getKey(), context.getToolType());
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ToolCallingObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = toolDefinitionDescription(keyValues, context);
        keyValues = toolCallId(keyValues, context);
        if (properties.isIncludeToolCallContent()) {
            keyValues = toolCallArguments(keyValues, context);
            keyValues = toolCallResult(keyValues, context);
        }
        return keyValues;
    }

    @Override
    protected KeyValues toolDefinitionDescription(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolDescription = context.getToolDefinition().description();
        return keyValues.and(
                GenAiIncubatingAttributes.GEN_AI_TOOL_DESCRIPTION.getKey(),
                toolDescription);
    }

    @Override
    protected KeyValues toolCallId(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallId = context.getToolCallId();
        return keyValues.and(
                GenAiIncubatingAttributes.GEN_AI_TOOL_CALL_ID.getKey(),
                toolCallId);
    }

    protected KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallArguments = context.getToolCallArguments();
        return keyValues.and(
                GenAiAttributes.GEN_AI_TOOL_CALL_ARGUMENTS.getKey(),
                toolCallArguments);
    }

    protected KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();
        if (toolCallResult != null) {
            return keyValues.and(
                    GenAiAttributes.GEN_AI_TOOL_CALL_RESULT.getKey(),
                    toolCallResult);
        }
        return keyValues;
    }

}
