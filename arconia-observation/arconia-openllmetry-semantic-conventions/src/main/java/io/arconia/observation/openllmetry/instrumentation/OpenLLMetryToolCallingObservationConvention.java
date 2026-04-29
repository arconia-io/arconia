package io.arconia.observation.openllmetry.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

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

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return KeyValues.of(traceloopSpanKind(), traceloopEntityName(context));
    }

    private KeyValue traceloopSpanKind() {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.SPAN_KIND_TOOL);
    }

    private KeyValue traceloopEntityName(ToolCallingObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, context.getToolDefinition().name());
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ToolCallingObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = entityInput(keyValues, context);
        keyValues = entityOutput(keyValues, context);
        return keyValues;
    }

    private KeyValues entityInput(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallArguments = context.getToolCallArguments();

        if (!openLLMetryOptions.isTraceContent()) {
            return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER);
        }

        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, toolCallArguments);
    }

    private KeyValues entityOutput(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();

        if (!openLLMetryOptions.isTraceContent()) {
            return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER);
        }

        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, toolCallResult != null ? toolCallResult : "{}");
    }

}
