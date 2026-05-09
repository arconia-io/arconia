package io.arconia.observation.openllmetry.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;

/**
 * {@link ToolCallingObservationConvention} for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public class OpenLLMetryToolCallingObservationConvention extends DefaultToolCallingObservationConvention {

    private final OpenLLMetryOptions openLLMetryOptions;

    public OpenLLMetryToolCallingObservationConvention(OpenLLMetryOptions openLLMetryOptions) {
        this.openLLMetryOptions = openLLMetryOptions;
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return KeyValues.of(aiOperationType(context), toolType(context), toolDefinitionName(context),
                traceloopSpanKind(), traceloopEntityName(context));
    }

    @Override
    protected KeyValue toolType(ToolCallingObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_TYPE.getKey(), context.getToolType());
    }

    @Override
    protected KeyValue toolDefinitionName(ToolCallingObservationContext context) {
        String toolName = context.getToolDefinition().name();
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_NAME.getKey(), toolName);
    }

    private KeyValue traceloopSpanKind() {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.TraceloopSpanKind.TOOL.getValue());
    }

    private KeyValue traceloopEntityName(ToolCallingObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, context.getToolDefinition().name());
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ToolCallingObservationContext context) {
        var keyValues = KeyValues.empty();
        // Metadata
        keyValues = toolDefinitionDescription(keyValues, context);
        // Content
        if (openLLMetryOptions.getToolExecution().isIncludeContent()) {
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
                OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT,
                toolCallArguments);
    }

    private KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();
        if (toolCallResult != null) {
            return keyValues.and(
                    OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT,
                    toolCallResult);
        }
        return keyValues;
    }

}
