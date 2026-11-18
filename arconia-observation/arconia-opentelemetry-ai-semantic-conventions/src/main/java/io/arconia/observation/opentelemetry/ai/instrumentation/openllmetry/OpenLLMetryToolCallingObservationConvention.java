package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import io.arconia.core.annotations.Incubating;
import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryToolCallingObservationConvention;

/**
 * OpenLLMetry flavor of {@link OpenTelemetryToolCallingObservationConvention}.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
@Incubating
public final class OpenLLMetryToolCallingObservationConvention extends OpenTelemetryToolCallingObservationConvention {

    public OpenLLMetryToolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ToolCallingObservationContext context) {
        return super.getHighCardinalityKeyValues(context).and(
                traceloopEntityName(context),
                traceloopSpanKind()
        );
    }

    private KeyValue traceloopEntityName(ToolCallingObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, context.getToolDefinition().name());
    }

    private KeyValue traceloopSpanKind() {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND,
                OpenLLMetryAttributes.TraceloopSpanKind.TOOL.getValue());
    }

    /**
     * Records the arguments both as {@code gen_ai.tool.call.arguments} and as {@code traceloop.entity.input},
     * as the OpenLLMetry instrumentations do.
     */
    @Override
    protected KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        return super.toolCallArguments(keyValues, context)
                .and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, context.getToolCallArguments());
    }

    /**
     * Records the result both as {@code gen_ai.tool.call.result} and as {@code traceloop.entity.output},
     * as the OpenLLMetry instrumentations do.
     */
    @Override
    protected KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();
        if (toolCallResult == null) {
            return keyValues;
        }
        return super.toolCallResult(keyValues, context)
                .and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, toolCallResult);
    }

}
