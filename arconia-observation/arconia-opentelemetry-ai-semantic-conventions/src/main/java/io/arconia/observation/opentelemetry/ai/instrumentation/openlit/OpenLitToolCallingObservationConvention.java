package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryToolCallingObservationConvention;

/**
 * OpenLIT flavor of {@link OpenTelemetryToolCallingObservationConvention}.
 *
 * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
 */
public final class OpenLitToolCallingObservationConvention extends OpenTelemetryToolCallingObservationConvention {

    public OpenLitToolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        super(properties);
    }

    @Override
    protected KeyValue aiProvider(ToolCallingObservationContext context) {
        return KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM,
                GenAiConventionsConverter.toOpenLitProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    protected KeyValues toolCallArguments(KeyValues keyValues, ToolCallingObservationContext context) {
        return keyValues.and(OpenLitAttributes.GEN_AI_TOOL_ARGS, context.getToolCallArguments());
    }

}
