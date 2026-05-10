package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

public class OpenTelemetryImageModelObservationConvention extends DefaultImageModelObservationConvention {

    @Override
    protected KeyValue aiOperationType(ImageModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                GenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(ImageModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ImageModelObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        keyValues = outputType(keyValues, context);
        return keyValues;
    }

    protected KeyValues outputType(KeyValues keyValues, ImageModelObservationContext context) {
        return keyValues.and(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey(),
                GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.IMAGE);
    }

}
