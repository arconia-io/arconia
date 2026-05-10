package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * OpenLLMetry flavor of {@link OpenTelemetryToolCallingObservationConvention}.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryToolCallingObservationConvention extends OpenTelemetryToolCallingObservationConvention {

    public OpenLLMetryToolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ToolCallingObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(
                traceloopEntityName(context),
                traceloopSpanKind()
        );
    }

    @Override
    protected KeyValue aiProvider(ToolCallingObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM,
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    private KeyValue traceloopEntityName(ToolCallingObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, context.getToolDefinition().name());
    }

    private KeyValue traceloopSpanKind() {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND,
                OpenLLMetryAttributes.TraceloopSpanKind.TOOL.getValue());
    }

    // HIGH CARDINALITY

    @Override
    protected KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallArguments = context.getToolCallArguments();
        return keyValues.and(
                OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT,
                toolCallArguments);
    }

    @Override
    protected KeyValues toolCallResult(KeyValues keyValues, ToolCallingObservationContext context) {
        String toolCallResult = context.getToolCallResult();
        if (toolCallResult != null) {
            return keyValues.and(
                    OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT,
                    toolCallResult);
        }
        return keyValues;
    }

}
